package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerBoolean;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEnum;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerFloat;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerOpaque;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerReference;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads Symfony log messages without retaining their potentially sensitive context values. */
public final class SymfonyProfilerLoggerConsumer {
    public static final SymfonyProfilerLoggerConsumer INSTANCE = new SymfonyProfilerLoggerConsumer();

    private static final Set<String> LOGGER_COLLECTORS = Set.of(
        "Symfony\\Component\\HttpKernel\\DataCollector\\LoggerDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\LoggerDataCollector"
    );

    private SymfonyProfilerLoggerConsumer() {
    }

    public SymfonyProfilerLogger read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("logger");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the logger collector");
        }
        String className = collector.getClassName().utf8StringOrNull();
        if (className == null || !LOGGER_COLLECTORS.contains(className)) {
            throw new IllegalStateException("Symfony logger collector has an unsupported class");
        }

        PhpValue rawData = collector.get("data");
        if (rawData == null) {
            throw new IllegalStateException("Symfony logger collector does not contain its data value");
        }
        if (!(new SymfonyVarDumperDataReader(profile.getResult()).read(rawData) instanceof ProfilerArray data)) {
            throw new IllegalStateException("Symfony logger collector data is not an array");
        }
        if (!(data.get("logs") instanceof ProfilerArray logs)) {
            return new SymfonyProfilerLogger(Collections.emptyList());
        }

        List<IndexedLog> indexedLogs = new ArrayList<>();
        List<ProfilerEntry> entries = logs.getEntries();
        for (int index = 0; index < entries.size(); index++) {
            if (!(entries.get(index).getValue() instanceof ProfilerArray log)) {
                continue;
            }
            indexedLogs.add(readLog(log, index));
        }
        indexedLogs.sort(
            Comparator.comparingLong(SymfonyProfilerLoggerConsumer::timestampOrMin).reversed()
                .thenComparing(Comparator.comparingInt(IndexedLog::sourceIndex).reversed())
        );

        List<SymfonyProfilerLog> result = new ArrayList<>(indexedLogs.size());
        for (IndexedLog indexedLog : indexedLogs) {
            result.add(indexedLog.log());
        }
        return new SymfonyProfilerLogger(result);
    }

    private static long timestampOrMin(IndexedLog indexedLog) {
        Long timestamp = indexedLog.log().getTimestampEpochMillis();
        return timestamp != null ? timestamp : Long.MIN_VALUE;
    }

    private IndexedLog readLog(ProfilerArray log, int sourceIndex) {
        int priority = 0;
        if (log.get("priority") instanceof ProfilerInteger priorityValue
            && priorityValue.getValue() >= Integer.MIN_VALUE && priorityValue.getValue() <= Integer.MAX_VALUE) {
            priority = (int) priorityValue.getValue();
        }
        String rawPriorityName = text(log, "priorityName");
        if (rawPriorityName == null) {
            rawPriorityName = text(log, "priority_name");
        }
        String priorityName = rawPriorityName != null ? rawPriorityName.toUpperCase(Locale.ROOT) : null;
        String rawType = text(log, "type");
        String type = rawType != null ? rawType.toLowerCase(Locale.ROOT) : null;
        Boolean scream = log.get("scream") instanceof ProfilerBoolean screamValue ? screamValue.getValue() : null;
        String timestamp = text(log, "timestamp_rfc3339");
        Long epochMillis = timestamp != null ? parseTimestamp(timestamp) : null;
        if (epochMillis == null) {
            Double number = number(log, "timestamp");
            if (number != null && Double.isFinite(number)) {
                epochMillis = (long) (number * 1000.0);
            }
        }

        String channel = text(log, "channel");
        if (channel != null && channel.isBlank()) {
            channel = null;
        }

        long occurrences = log.get("errorCount") instanceof ProfilerInteger errorCount
            ? Math.max(errorCount.getValue(), 1L)
            : 1L;

        return new IndexedLog(
            sourceIndex,
            new SymfonyProfilerLog(
                logSection(priority, priorityName, type, scream),
                timestamp != null ? timestamp : (epochMillis != null ? Instant.ofEpochMilli(epochMillis).toString() : null),
                epochMillis,
                priority,
                channel,
                logMessage(log.get("message")),
                occurrences
            )
        );
    }

    private SymfonyProfilerLogSection logSection(
        int priority,
        @Nullable String priorityName,
        @Nullable String type,
        @Nullable Boolean scream
    ) {
        if ("deprecation".equals(type) || (priority <= 300 && Boolean.FALSE.equals(scream))) {
            return SymfonyProfilerLogSection.DEPRECATION;
        }
        if ("scream".equals(type) || "silenced".equals(type) || (priority <= 300 && Boolean.TRUE.equals(scream))) {
            return SymfonyProfilerLogSection.SILENCED;
        }

        if (priorityName == null) {
            return sectionFromPriority(priority);
        }

        return switch (priorityName) {
            case "EMERGENCY" -> SymfonyProfilerLogSection.EMERGENCY;
            case "ALERT" -> SymfonyProfilerLogSection.ALERT;
            case "CRITICAL" -> SymfonyProfilerLogSection.CRITICAL;
            case "ERROR" -> SymfonyProfilerLogSection.ERROR;
            case "WARNING" -> SymfonyProfilerLogSection.WARNING;
            case "NOTICE" -> SymfonyProfilerLogSection.NOTICE;
            case "INFO" -> SymfonyProfilerLogSection.INFO;
            case "DEBUG" -> SymfonyProfilerLogSection.DEBUG;
            default -> sectionFromPriority(priority);
        };
    }

    private SymfonyProfilerLogSection sectionFromPriority(int priority) {
        if (priority >= 600) {
            return SymfonyProfilerLogSection.EMERGENCY;
        }
        if (priority >= 550) {
            return SymfonyProfilerLogSection.ALERT;
        }
        if (priority >= 500) {
            return SymfonyProfilerLogSection.CRITICAL;
        }
        if (priority >= 400) {
            return SymfonyProfilerLogSection.ERROR;
        }
        if (priority >= 300) {
            return SymfonyProfilerLogSection.WARNING;
        }
        if (priority >= 250) {
            return SymfonyProfilerLogSection.NOTICE;
        }
        if (priority >= 200) {
            return SymfonyProfilerLogSection.INFO;
        }
        return SymfonyProfilerLogSection.DEBUG;
    }

    private static @Nullable Long parseTimestamp(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        }
        catch (DateTimeParseException e) {
            return null;
        }
    }

    private String logMessage(@Nullable ProfilerValue value) {
        if (value == null || value instanceof ProfilerNull) {
            return "none";
        }
        if (value instanceof ProfilerString string) {
            String text = string.utf8StringOrNull();
            return text != null ? text : "[binary log message, " + string.getBytes().getSize() + " bytes]";
        }
        if (value instanceof ProfilerBoolean booleanValue) {
            return String.valueOf(booleanValue.getValue());
        }
        if (value instanceof ProfilerInteger integer) {
            return String.valueOf(integer.getValue());
        }
        if (value instanceof ProfilerFloat floatValue) {
            return String.valueOf(floatValue.getValue());
        }
        if (value instanceof ProfilerArray) {
            return "[structured log message]";
        }
        if (value instanceof ProfilerEnum enumValue) {
            String enumName = enumValue.getEnumName().utf8StringOrNull();
            String caseName = enumValue.getCaseName().utf8StringOrNull();
            return (enumName != null ? enumName : "binary enum") + "::" + (caseName != null ? caseName : "binary case");
        }
        if (value instanceof ProfilerReference reference) {
            return "[reference to position " + reference.getPosition() + "]";
        }

        // ProfilerValue is sealed: the remaining kind is ProfilerOpaque
        return "[opaque " + ((ProfilerOpaque) value).getKind() + "]";
    }

    private static @Nullable String text(ProfilerArray array, String key) {
        return array.get(key) instanceof ProfilerString string ? string.utf8StringOrNull() : null;
    }

    private static @Nullable Double number(ProfilerArray array, String key) {
        ProfilerValue value = array.get(key);
        if (value instanceof ProfilerFloat floatValue) {
            return floatValue.getValue();
        }
        if (value instanceof ProfilerInteger integer) {
            return (double) integer.getValue();
        }
        return null;
    }

    private record IndexedLog(
        int sourceIndex,
        SymfonyProfilerLog log
    ) {
    }
}
