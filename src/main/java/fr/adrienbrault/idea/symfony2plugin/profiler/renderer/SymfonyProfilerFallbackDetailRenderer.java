package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollectorConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;

import java.util.regex.Pattern;

/**
 * Renders bounded and sanitized data for collectors without a specialized renderer.
 */
public final class SymfonyProfilerFallbackDetailRenderer {
    public static final SymfonyProfilerFallbackDetailRenderer INSTANCE = new SymfonyProfilerFallbackDetailRenderer();

    private static final Pattern FALLBACK_CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerFallbackDetailRenderer() {
    }

    public String renderDetails(SymfonyProfilerProfile profile, String collectorName, int page) {
        return formatDetails(SymfonyProfilerFallbackCollectorConsumer.INSTANCE.read(profile, collectorName), page);
    }

    public String formatDetails(SymfonyProfilerFallbackCollector collector, int page) {
        ProfilerDetailPage detailPage = ProfilerDetailRendererKt.paginateProfilerDetailEntries(
            ProfilerTextRenderer.INSTANCE.render(collector.getData()),
            page
        );

        StringBuilder builder = new StringBuilder();
        builder.append("Collector: ").append(renderCollectorName(collector.getName())).append('\n');
        if (detailPage.isPaginated()) {
            builder.append("Page: ").append(detailPage.getNumber()).append(" of ").append(detailPage.getTotal()).append('\n');
        }
        builder.append('\n');
        builder.append("## Data").append('\n');
        builder.append('\n');
        for (String entry : detailPage.getEntries()) {
            builder.append(entry).append('\n');
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Kotlin default argument {@code page = 1}.
     */
    public String formatDetails(SymfonyProfilerFallbackCollector collector) {
        return formatDetails(collector, 1);
    }

    private static String renderCollectorName(String name) {
        return FALLBACK_CONTROL_CHARACTERS.matcher(name).replaceAll(" ").replace("|", "\\|");
    }
}
