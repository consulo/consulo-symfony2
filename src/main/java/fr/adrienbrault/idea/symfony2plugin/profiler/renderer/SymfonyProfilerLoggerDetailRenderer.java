package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLog;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogSection;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogger;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLoggerConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders prioritized and bounded log sections without retaining log context.
 */
public final class SymfonyProfilerLoggerDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerLoggerDetailRenderer INSTANCE = new SymfonyProfilerLoggerDetailRenderer();

    private static final int OVERVIEW_LOG_LIMIT = 5;
    private static final int DETAIL_LOG_LIMIT = 25;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private SymfonyProfilerLoggerDetailRenderer() {
    }

    @Override
    public String getName() {
        return "logger";
    }

    @Override
    public int getOverviewWeight() {
        return 40;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerLoggerConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerLoggerConsumer.INSTANCE.read(profile));
    }

    /**
     * Renders at most five high-priority log entries across all important sections.
     */
    public String formatOverview(SymfonyProfilerLogger logger) {
        StringBuilder builder = new StringBuilder();
        List<SymfonyProfilerLog> importantLogs = new ArrayList<>();
        for (SymfonyProfilerLogSection section : SymfonyProfilerLogSection.values()) {
            if (!section.getImportant()) {
                continue;
            }
            for (SymfonyProfilerLog log : logger.getLogs()) {
                if (log.getSection() == section) {
                    importantLogs.add(log);
                }
            }
        }

        builder.append("## Collector: logger").append('\n');
        builder.append('\n');
        builder.append("- Log entries: ").append(logger.getLogs().size()).append('\n');
        builder.append('\n');
        builder.append("### Important logs (").append(formatEntryCount(importantLogs.size())).append(")").append('\n');

        if (importantLogs.isEmpty()) {
            builder.append('\n');
            builder.append("No important log messages recorded.").append('\n');
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        builder.append('\n');
        appendLogTable(builder, take(importantLogs, OVERVIEW_LOG_LIMIT), true);
        if (importantLogs.size() > OVERVIEW_LOG_LIMIT) {
            builder.append('\n');
            builder.append(
                "_Truncated: showing " + OVERVIEW_LOG_LIMIT + " of " + importantLogs.size() + " important log entries._"
            ).append('\n');
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Renders up to 25 newest entries for every non-empty log section.
     */
    public String formatDetails(SymfonyProfilerLogger logger) {
        StringBuilder builder = new StringBuilder();
        builder.append("## Collector: logger").append('\n');
        builder.append('\n');
        builder.append("- Log entries: ").append(logger.getLogs().size()).append('\n');

        if (logger.getLogs().isEmpty()) {
            builder.append('\n');
            builder.append("No log messages available.").append('\n');
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        for (SymfonyProfilerLogSection section : SymfonyProfilerLogSection.values()) {
            List<SymfonyProfilerLog> logs = new ArrayList<>();
            for (SymfonyProfilerLog log : logger.getLogs()) {
                if (log.getSection() == section) {
                    logs.add(log);
                }
            }
            if (logs.isEmpty()) {
                continue;
            }

            builder.append('\n');
            builder.append("### ").append(section.getTitle()).append(" (").append(formatEntryCount(logs.size())).append(")").append('\n');
            builder.append('\n');
            appendLogTable(builder, take(logs, DETAIL_LOG_LIMIT), false);
            if (logs.size() > DETAIL_LOG_LIMIT) {
                builder.append('\n');
                builder.append(
                    "_Truncated: showing the newest " + DETAIL_LOG_LIMIT + " of " + logs.size() + " entries._"
                ).append('\n');
            }
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    private static void appendLogTable(StringBuilder builder, List<SymfonyProfilerLog> logs, boolean includeLevel) {
        if (includeLevel) {
            builder.append("| Level | Time | Channel | Occurrences | Message |").append('\n');
            builder.append("| --- | --- | --- | ---: | --- |").append('\n');
        }
        else {
            builder.append("| Time | Channel | Occurrences | Message |").append('\n');
            builder.append("| --- | --- | ---: | --- |").append('\n');
        }
        for (SymfonyProfilerLog log : logs) {
            String prefix = includeLevel ? "| " + plainText(log.getSection().getTitle()) + " " : "";
            String channel = log.getChannel();
            builder.append(
                prefix + "| " + plainText(formatLogTimestamp(log.getTimestamp())) + " | " +
                    plainText(channel != null ? channel : "none") + " | " + log.getOccurrences() + " | " + plainText(log.getMessage()) + " |"
            ).append('\n');
        }
    }

    private static <T> List<T> take(List<T> list, int count) {
        return list.size() <= count ? list : list.subList(0, count);
    }

    private static String formatEntryCount(int count) {
        return count + " " + (count == 1 ? "entry" : "entries");
    }

    private static String formatLogTimestamp(@Nullable String value) {
        if (value == null) {
            return "unknown";
        }

        try {
            return OffsetDateTime.parse(value).format(LOG_TIME_FORMATTER);
        }
        catch (DateTimeParseException e) {
            return value;
        }
    }

    /**
     * Keeps untrusted log values on one table-safe line.
     */
    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
