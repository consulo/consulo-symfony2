package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabase;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseStackFrame;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders Doctrine query summaries and paginated query groups.
 */
public final class SymfonyProfilerDatabaseDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerDatabaseDetailRenderer INSTANCE = new SymfonyProfilerDatabaseDetailRenderer();

    private static final int OVERVIEW_QUERY_GROUP_LIMIT = 3;
    private static final int CALL_LIMIT = 5;
    private static final Set<String> INTERNAL_CALL_ROOT_NAMESPACES = Set.of("doctrine", "symfony");
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerDatabaseDetailRenderer() {
    }

    @Override
    public String getName() {
        return "db";
    }

    @Override
    public int getOverviewWeight() {
        return 70;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerDatabaseConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerDatabaseConsumer.INSTANCE.read(profile), page);
    }

    /**
     * Renders the compact database summary with its top query groups.
     */
    public String formatOverview(SymfonyProfilerDatabase database) {
        return formatSection(
            database,
            database.getQueryGroups().stream()
                .limit(OVERVIEW_QUERY_GROUP_LIMIT)
                .map(SymfonyProfilerDatabaseDetailRenderer::formatQueryGroup)
                .toList(),
            "### Top " + OVERVIEW_QUERY_GROUP_LIMIT + " query groups"
        );
    }

    /**
     * Renders one size-targeted page while keeping every database query group intact.
     */
    public String formatDetails(SymfonyProfilerDatabase database, int page) {
        ProfilerDetailPage detailPage = ProfilerDetailRendererKt.paginateProfilerDetailEntries(
            database.getQueryGroups().stream().map(SymfonyProfilerDatabaseDetailRenderer::formatQueryGroup).toList(),
            page
        );
        String heading;
        if (detailPage.isPaginated()) {
            heading = "### Query groups (page " + detailPage.getNumber() + " of " + detailPage.getTotal() + ")";
        }
        else {
            heading = "### Query groups";
        }

        return formatSection(database, detailPage.getEntries(), heading);
    }

    /**
     * Kotlin default argument {@code page = 1}.
     */
    public String formatDetails(SymfonyProfilerDatabase database) {
        return formatDetails(database, 1);
    }

    private String formatSection(SymfonyProfilerDatabase database, List<String> queryRows, String heading) {
        StringBuilder builder = new StringBuilder();
        builder.append("## Collector: db").append('\n');
        builder.append('\n');
        builder.append("- Queries: ").append(database.getQueryCount()).append('\n');
        builder.append("- Query time: ").append(formatMilliseconds(database.getTotalTimeMs())).append(" ms").append('\n');
        builder.append("- Duplicate query groups: ").append(database.getDuplicateQueryCount()).append('\n');
        builder.append(
            "- Connections: " +
                (database.getConnections().isEmpty()
                    ? "none"
                    : database.getConnections().stream().map(SymfonyProfilerDatabaseDetailRenderer::plainText).collect(Collectors.joining(", ")))
        ).append('\n');
        builder.append('\n');
        builder.append(heading).append('\n');

        if (queryRows.isEmpty()) {
            builder.append('\n');
            builder.append("No queries recorded.").append('\n');
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        builder.append('\n');
        builder.append("| Occurrences | Time (ms) | Average time (ms) | Query | Calls |").append('\n');
        builder.append("| ---: | ---: | ---: | --- | --- |").append('\n');
        for (String queryRow : queryRows) {
            builder.append(queryRow).append('\n');
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    private static String formatQueryGroup(SymfonyProfilerDatabaseQueryGroup group) {
        return "| " + group.getCount() + " | " + formatMilliseconds(group.getTotalTimeMs()) + " | " +
            formatMilliseconds(group.getAverageTimeMs()) + " | " + plainText(group.getSql()) + " | " +
            plainText(formatCalls(group)) + " |";
    }

    /**
     * Reduces all occurrence traces to unique non-framework calls in their original order.
     */
    private static String formatCalls(SymfonyProfilerDatabaseQueryGroup group) {
        Set<String> calls = new LinkedHashSet<>();
        outer:
        for (List<SymfonyProfilerDatabaseStackFrame> stackTrace : group.getStackTraces()) {
            for (SymfonyProfilerDatabaseStackFrame frame : stackTrace) {
                String call = formatCall(frame);
                if (call == null) {
                    continue;
                }

                calls.add(call);
                if (calls.size() >= CALL_LIMIT) {
                    break outer;
                }
            }
        }

        return String.join(", ", calls);
    }

    private static @Nullable String formatCall(SymfonyProfilerDatabaseStackFrame frame) {
        String rawClassName = frame.getClassName();
        if (rawClassName == null) {
            return null;
        }

        String className = trimStart(ProfilerDetailRendererKt.trim(rawClassName), '\\');
        if (className.isEmpty()) {
            return null;
        }

        String rootNamespace = substringBefore(className, '\\').toLowerCase(Locale.ROOT);
        if (INTERNAL_CALL_ROOT_NAMESPACES.contains(rootNamespace)) {
            return null;
        }

        String rawMethod = frame.getFunction();
        if (rawMethod == null) {
            return null;
        }

        String method = ProfilerDetailRendererKt.trim(rawMethod);
        if (method.isEmpty()) {
            return null;
        }

        String shortClassName = substringAfterLast(className, '\\');
        if (shortClassName.isEmpty()) {
            return null;
        }

        return shortClassName + ":" + method;
    }

    private static String trimStart(String value, char character) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == character) {
            start++;
        }
        return value.substring(start);
    }

    private static String substringBefore(String value, char delimiter) {
        int index = value.indexOf(delimiter);
        return index == -1 ? value : value.substring(0, index);
    }

    private static String substringAfterLast(String value, char delimiter) {
        int index = value.lastIndexOf(delimiter);
        return index == -1 ? value : value.substring(index + 1);
    }

    private static String formatMilliseconds(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
