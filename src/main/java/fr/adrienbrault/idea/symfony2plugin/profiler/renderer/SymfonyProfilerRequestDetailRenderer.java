package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestSummary;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Renders sanitized request metadata and the generic request value tree.
 */
public final class SymfonyProfilerRequestDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerRequestDetailRenderer INSTANCE = new SymfonyProfilerRequestDetailRenderer();

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerRequestDetailRenderer() {
    }

    @Override
    public String getName() {
        return "request";
    }

    @Override
    public int getOverviewWeight() {
        return 100;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        Long time = profile.getTime();
        return formatOverview(
            SymfonyProfilerRequestConsumer.INSTANCE.read(profile),
            time != null ? ProfilerTextRenderer.INSTANCE.formatTimestamp(time) : null
        );
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerRequestConsumer.INSTANCE.read(profile), page);
    }

    /**
     * Renders only stable request metadata in the compact profile overview.
     */
    public String formatOverview(SymfonyProfilerRequest request, @Nullable String profiledAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("## Collector: request").append('\n');
        builder.append('\n');
        if (profiledAt != null) {
            builder.append("- Profiled at: ").append(profiledAt).append('\n');
        }
        builder.append("- Path: ").append(renderOverviewValue(request.getSummary().getPath())).append('\n');
        builder.append("- Route: ").append(renderOverviewValue(request.getSummary().getRoute())).append('\n');
        builder.append("- Content type: ").append(renderOverviewValue(request.getSummary().getContentType())).append('\n');
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Kotlin default argument {@code profiledAt = null}.
     */
    public String formatOverview(SymfonyProfilerRequest request) {
        return formatOverview(request, null);
    }

    /**
     * Renders a bounded page of the sanitized generic request value tree.
     */
    public String formatDetails(SymfonyProfilerRequest request, int page) {
        ProfilerDetailPage detailPage = ProfilerDetailRendererKt.paginateProfilerDetailEntries(
            ProfilerTextRenderer.INSTANCE.render(request.getData()),
            page
        );

        StringBuilder builder = new StringBuilder();
        builder.append("Collector: request").append('\n');
        if (detailPage.isPaginated()) {
            builder.append("Page: ").append(detailPage.getNumber()).append(" of ").append(detailPage.getTotal()).append('\n');
        }
        builder.append('\n');
        builder.append("Summary").append('\n');
        builder.append('\n');
        appendRequestSummary(builder, request.getSummary());
        builder.append('\n');
        builder.append("Data").append('\n');
        builder.append('\n');
        for (String entry : detailPage.getEntries()) {
            builder.append(entry).append('\n');
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Kotlin default argument {@code page = 1}.
     */
    public String formatDetails(SymfonyProfilerRequest request) {
        return formatDetails(request, 1);
    }

    private static void appendRequestSummary(StringBuilder builder, SymfonyProfilerRequestSummary summary) {
        Integer statusCode = summary.getStatusCode();
        builder.append("Method: ").append(renderSummaryValue(summary.getMethod())).append('\n');
        builder.append("Path: ").append(renderSummaryValue(summary.getPath())).append('\n');
        builder.append("Route: ").append(renderSummaryValue(summary.getRoute())).append('\n');
        builder.append("Status: ").append(statusCode != null ? statusCode.toString() : "none").append('\n');
        builder.append("Content type: ").append(renderSummaryValue(summary.getContentType())).append('\n');
    }

    private static String renderSummaryValue(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ");
    }

    private static String renderOverviewValue(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }
        return plainText(value);
    }

    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
