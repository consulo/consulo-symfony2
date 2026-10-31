package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValueKt;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Doctrine query information from a validated Symfony profile.
 * Formatting stays outside so other profiler collectors can contribute their own sections.
 */
public final class SymfonyProfilerDatabaseConsumer {
    public static final SymfonyProfilerDatabaseConsumer INSTANCE = new SymfonyProfilerDatabaseConsumer();

    private static final String DOCTRINE_COLLECTOR =
        "Doctrine\\Bundle\\DoctrineBundle\\DataCollector\\DoctrineDataCollector";

    private SymfonyProfilerDatabaseConsumer() {
    }

    public SymfonyProfilerDatabase read(SymfonyProfilerProfile profile) {
        PhpArray collectorData = findDoctrineCollectorData(profile);
        List<String> connections = readConnections(collectorData, profile);
        List<Query> queries = readQueries(collectorData, profile);
        List<SymfonyProfilerDatabaseQueryGroup> groupedQueries = groupQueries(queries);

        double executionSeconds = 0.0;
        for (Query query : queries) {
            executionSeconds += query.executionSeconds();
        }
        double totalTimeMs = roundToTwoDecimals(executionSeconds * 1000);

        int duplicateQueryCount = 0;
        for (SymfonyProfilerDatabaseQueryGroup group : groupedQueries) {
            if (group.getCount() > 1) {
                duplicateQueryCount++;
            }
        }

        return new SymfonyProfilerDatabase(
            queries.size(),
            totalTimeMs,
            connections,
            duplicateQueryCount,
            groupedQueries
        );
    }

    private PhpArray findDoctrineCollectorData(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("db");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the db collector");
        }

        if (!DOCTRINE_COLLECTOR.equals(collector.getClassName().utf8StringOrNull())) {
            throw new IllegalStateException("Symfony db collector is not a DoctrineDataCollector");
        }

        if (SymfonyProfilerProfileKt.resolve(collector.get("data"), profile.getResult()) instanceof PhpArray data) {
            return data;
        }
        throw new IllegalStateException("Doctrine collector does not contain its data array");
    }

    private List<String> readConnections(PhpArray data, SymfonyProfilerProfile profile) {
        if (!(SymfonyProfilerProfileKt.resolve(data.get("connections"), profile.getResult()) instanceof PhpArray connections)) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        for (PhpArrayEntry entry : connections.getEntries()) {
            if (entry.getKey() instanceof PhpStringKey stringKey) {
                String name = stringKey.getBytes().utf8StringOrNull();
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private List<Query> readQueries(PhpArray data, SymfonyProfilerProfile profile) {
        PhpUnserializeResult result = profile.getResult();
        if (!(SymfonyProfilerProfileKt.resolve(data.get("queries"), result) instanceof PhpArray connections)) {
            return Collections.emptyList();
        }

        List<Query> list = new ArrayList<>();
        for (PhpArrayEntry connection : connections.getEntries()) {
            if (!(SymfonyProfilerProfileKt.resolve(connection.getValue(), result) instanceof PhpArray queries)) {
                throw new IllegalStateException("Doctrine connection queries must be a PHP array");
            }
            for (PhpArrayEntry entry : queries.getEntries()) {
                if (!(SymfonyProfilerProfileKt.resolve(entry.getValue(), result) instanceof PhpArray query)) {
                    throw new IllegalStateException("Doctrine query must be a PHP array");
                }
                String sql = utf8StringOrNull(SymfonyProfilerProfileKt.resolve(query.get("sql"), result));
                list.add(new Query(
                    sql != null ? sql : "",
                    numberOrZero(SymfonyProfilerProfileKt.resolve(query.get("executionMS"), result)),
                    readStackTrace(query, profile)
                ));
            }
        }
        return list;
    }

    private List<SymfonyProfilerDatabaseStackFrame> readStackTrace(
        PhpArray query,
        SymfonyProfilerProfile profile
    ) {
        PhpUnserializeResult result = profile.getResult();
        if (!(SymfonyProfilerProfileKt.resolve(query.get("backtrace"), result) instanceof PhpArray backtrace)) {
            return Collections.emptyList();
        }

        // Frame arguments may contain application data and are deliberately not retained.
        List<SymfonyProfilerDatabaseStackFrame> frames = new ArrayList<>();
        for (PhpArrayEntry entry : backtrace.getEntries()) {
            if (!(SymfonyProfilerProfileKt.resolve(entry.getValue(), result) instanceof PhpArray frame)) {
                continue;
            }

            Integer line = null;
            if (SymfonyProfilerProfileKt.resolve(frame.get("line"), result) instanceof PhpInteger lineValue
                && lineValue.getValue() >= Integer.MIN_VALUE && lineValue.getValue() <= Integer.MAX_VALUE) {
                line = (int) lineValue.getValue();
            }

            frames.add(new SymfonyProfilerDatabaseStackFrame(
                utf8StringOrNull(SymfonyProfilerProfileKt.resolve(frame.get("file"), result)),
                line,
                utf8StringOrNull(SymfonyProfilerProfileKt.resolve(frame.get("class"), result)),
                utf8StringOrNull(SymfonyProfilerProfileKt.resolve(frame.get("function"), result)),
                utf8StringOrNull(SymfonyProfilerProfileKt.resolve(frame.get("type"), result))
            ));
        }
        return frames;
    }

    private List<SymfonyProfilerDatabaseQueryGroup> groupQueries(List<Query> queries) {
        Map<String, MutableQueryGroup> grouped = new LinkedHashMap<>();
        for (Query query : queries) {
            MutableQueryGroup group = grouped.computeIfAbsent(query.sql(), MutableQueryGroup::new);
            group.myCount++;
            group.myTotalTimeMs += query.executionSeconds() * 1000;
            group.myStackTraces.add(query.stackTrace());
        }

        List<SymfonyProfilerDatabaseQueryGroup> groups = new ArrayList<>(grouped.size());
        for (MutableQueryGroup group : grouped.values()) {
            double totalTimeMs = roundToTwoDecimals(group.myTotalTimeMs);
            groups.add(new SymfonyProfilerDatabaseQueryGroup(
                group.mySql,
                group.myCount,
                totalTimeMs,
                roundToTwoDecimals(totalTimeMs / group.myCount),
                new ArrayList<>(group.myStackTraces)
            ));
        }
        groups.sort(Comparator.comparingDouble(SymfonyProfilerDatabaseQueryGroup::getTotalTimeMs).reversed());
        return groups;
    }

    private static double numberOrZero(@Nullable PhpValue value) {
        if (value instanceof PhpFloat floatValue) {
            return floatValue.getValue();
        }
        if (value instanceof PhpInteger integer) {
            return (double) integer.getValue();
        }
        return 0.0;
    }

    private static @Nullable String utf8StringOrNull(@Nullable PhpValue value) {
        return value != null ? PhpValueKt.utf8StringOrNull(value) : null;
    }

    private static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record Query(
        String sql,
        double executionSeconds,
        List<SymfonyProfilerDatabaseStackFrame> stackTrace
    ) {
    }

    private static final class MutableQueryGroup {
        private final String mySql;
        private int myCount = 0;
        private double myTotalTimeMs = 0.0;
        private final List<List<SymfonyProfilerDatabaseStackFrame>> myStackTraces = new ArrayList<>();

        private MutableQueryGroup(String sql) {
            mySql = sql;
        }
    }
}
