package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerMemory;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Renders Symfony Stopwatch events as CSV ordered by descending duration.
 */
public final class SymfonyProfilerTimeDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerTimeDetailRenderer INSTANCE = new SymfonyProfilerTimeDetailRenderer();

    private static final int OVERVIEW_EVENT_LIMIT = 3;
    private static final double EVENT_THRESHOLD_MS = 1.0;

    private SymfonyProfilerTimeDetailRenderer() {
    }

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public int getOverviewWeight() {
        return 80;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerTimeConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerTimeConsumer.INSTANCE.read(profile));
    }

    /**
     * Renders the three slowest events meeting Symfony's default timeline threshold.
     */
    public String formatOverview(SymfonyProfilerTime time) {
        List<SymfonyProfilerTimeEvent> visibleEvents = visibleEvents(time);
        return formatSection(
            time,
            visibleEvents.size() <= OVERVIEW_EVENT_LIMIT ? visibleEvents : visibleEvents.subList(0, OVERVIEW_EVENT_LIMIT),
            "### Top " + OVERVIEW_EVENT_LIMIT + " events by duration (CSV)"
        );
    }

    /**
     * Renders all events meeting Symfony's default timeline threshold without pagination.
     */
    public String formatDetails(SymfonyProfilerTime time) {
        return formatSection(
            time,
            visibleEvents(time),
            "### Events ordered by duration (CSV)"
        );
    }

    private static List<SymfonyProfilerTimeEvent> visibleEvents(SymfonyProfilerTime time) {
        return time.getEvents().stream()
            .filter(it -> it.getDurationMs() >= EVENT_THRESHOLD_MS)
            .sorted(Comparator.comparingDouble(SymfonyProfilerTimeEvent::getDurationMs).reversed())
            .toList();
    }

    private static String formatSection(SymfonyProfilerTime time, List<SymfonyProfilerTimeEvent> events, String heading) {
        StringBuilder builder = new StringBuilder();
        builder.append("## Collector: time").append('\n');
        builder.append('\n');
        builder.append("- Total duration: ").append(formatMilliseconds(time.getDurationMs())).append(" ms").append('\n');
        builder.append("- Initialization time: ").append(formatMilliseconds(time.getInitializationTimeMs())).append(" ms").append('\n');
        builder.append("- Stopwatch installed: ").append(time.getStopwatchInstalled() ? "yes" : "no").append('\n');
        builder.append("- Events: ").append(time.getEvents().size()).append('\n');
        builder.append("- Threshold: ").append(formatMilliseconds(EVENT_THRESHOLD_MS)).append(" ms").append('\n');
        SymfonyProfilerMemory memory = time.getMemory();
        if (memory != null) {
            builder.append('\n');
            builder.append("### Memory").append('\n');
            builder.append('\n');
            builder.append("- Memory peak: ").append(formatMemoryMiB(memory.getPeakBytes())).append(" MiB").append('\n');
            builder.append("- PHP memory limit: ").append(formatMemoryLimit(memory.getLimitBytes())).append('\n');
        }
        builder.append('\n');
        builder.append(heading).append('\n');

        if (events.isEmpty()) {
            builder.append('\n');
            builder.append(
                time.getEvents().isEmpty()
                    ? "No timing events recorded."
                    : "No timing events meet the " + formatMilliseconds(EVENT_THRESHOLD_MS) + " ms threshold."
            ).append('\n');
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        builder.append('\n');
        builder.append("event,category,start_ms,end_ms,duration_ms,memory_mib").append('\n');
        for (SymfonyProfilerTimeEvent event : events) {
            builder.append(
                ProfilerTextRenderer.INSTANCE.csvRow(
                    event.getName(),
                    event.getCategory(),
                    formatMilliseconds(event.getStartMs()),
                    formatMilliseconds(event.getEndMs()),
                    formatMilliseconds(event.getDurationMs()),
                    formatMemoryMiB(event.getMemoryBytes())
                )
            ).append('\n');
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    private static String formatMilliseconds(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatMemoryMiB(long bytes) {
        return BigDecimal.valueOf(bytes)
            .divide(BigDecimal.valueOf(1024L * 1024L), 2, RoundingMode.HALF_UP)
            .toPlainString();
    }

    private static String formatMemoryLimit(long bytes) {
        return bytes < 0 ? "unlimited" : formatMemoryMiB(bytes) + " MiB";
    }
}
