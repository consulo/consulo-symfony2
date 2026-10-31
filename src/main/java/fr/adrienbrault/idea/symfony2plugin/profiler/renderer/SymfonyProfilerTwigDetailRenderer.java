package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwig;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders Twig metrics, templates as CSV, and the complete rendering call tree.
 */
public final class SymfonyProfilerTwigDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerTwigDetailRenderer INSTANCE = new SymfonyProfilerTwigDetailRenderer();

    private static final int OVERVIEW_TEMPLATE_LIMIT = 5;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerTwigDetailRenderer() {
    }

    @Override
    public String getName() {
        return "twig";
    }

    @Override
    public int getOverviewWeight() {
        return 60;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerTwigConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerTwigConsumer.INSTANCE.read(profile));
    }

    /**
     * Renders the first five unique templates in their initial rendering order.
     */
    public String formatOverview(SymfonyProfilerTwig twig) {
        List<SymfonyProfilerTwigTemplate> templates = twig.getRenderedTemplates();
        return formatSection(
            twig,
            templates.size() <= OVERVIEW_TEMPLATE_LIMIT ? templates : templates.subList(0, OVERVIEW_TEMPLATE_LIMIT),
            "### First " + OVERVIEW_TEMPLATE_LIMIT + " rendered templates (CSV)",
            false
        );
    }

    /**
     * Renders all unique templates and the complete rendering call tree without pagination.
     */
    public String formatDetails(SymfonyProfilerTwig twig) {
        return formatSection(
            twig,
            twig.getRenderedTemplates(),
            "### Rendered templates (CSV)",
            true
        );
    }

    private static String formatSection(
        SymfonyProfilerTwig twig,
        List<SymfonyProfilerTwigTemplate> templates,
        String heading,
        boolean includeCallTree
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("## Collector: twig").append('\n');
        builder.append('\n');
        builder.append("- Render time: ").append(formatMilliseconds(twig.getRenderTimeMs())).append(" ms").append('\n');
        builder.append("- Template calls: ").append(twig.getTemplateCallCount()).append('\n');
        builder.append("- Block calls: ").append(twig.getBlockCallCount()).append('\n');
        builder.append("- Macro calls: ").append(twig.getMacroCallCount()).append('\n');
        builder.append("- Unique templates: ").append(twig.getRenderedTemplates().size()).append('\n');
        builder.append('\n');
        builder.append(heading).append('\n');

        if (templates.isEmpty()) {
            builder.append('\n');
            builder.append("No Twig templates were rendered.").append('\n');
        }
        else {
            builder.append('\n');
            builder.append("template,path,render_count").append('\n');
            for (SymfonyProfilerTwigTemplate template : templates) {
                String path = template.getPath();
                builder.append(
                    ProfilerTextRenderer.INSTANCE.csvRow(
                        template.getName(),
                        path != null ? path : "",
                        String.valueOf(template.getRenderCount())
                    )
                ).append('\n');
            }
        }

        if (includeCallTree) {
            builder.append('\n');
            builder.append("### Rendering call tree").append('\n');
            builder.append('\n');
            appendTwigProfile(builder, twig.getRoot(), twig.getRenderTimeMs(), "", false);
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    private static void appendTwigProfile(
        StringBuilder builder,
        SymfonyProfilerTwigProfile profile,
        double rootDurationMs,
        String prefix,
        boolean hasFollowingSibling
    ) {
        String label = switch (profile.getType()) {
            case "ROOT" -> treeText(profile.getName());
            case "template" -> prefix + "└ " + treeText(profile.getTemplate());
            default -> prefix + "└ " + treeText(profile.getTemplate()) + "::" + treeText(profile.getType()) + "(" + treeText(profile.getName()) + ")";
        };
        String timing;
        if (profile.getDurationMs() >= 1.0) {
            double percentage = rootDurationMs > 0.0 ? profile.getDurationMs() / rootDurationMs * 100.0 : 0.0;
            timing = " " + formatMilliseconds(profile.getDurationMs()) + "ms/" + formatPercentage(percentage) + "%";
        }
        else {
            timing = "";
        }
        builder.append("    ").append(label).append(timing).append('\n');

        String childPrefix;
        if (profile.getType().equals("ROOT")) {
            childPrefix = prefix;
        }
        else {
            childPrefix = prefix + (hasFollowingSibling ? "│ " : "  ");
        }
        List<SymfonyProfilerTwigProfile> children = profile.getChildren();
        for (int index = 0; index < children.size(); index++) {
            appendTwigProfile(
                builder,
                children.get(index),
                rootDurationMs,
                childPrefix,
                index + 1 < children.size()
            );
        }
    }

    private static String formatMilliseconds(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatPercentage(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String treeText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ");
    }
}
