package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Composes the request header and all available collector renderers.
 */
public final class SymfonyProfilerRequestDetailsRenderer {
    public static final SymfonyProfilerRequestDetailsRenderer INSTANCE = new SymfonyProfilerRequestDetailsRenderer();

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private final List<ProfilerDetailRenderer> renderers;

    private SymfonyProfilerRequestDetailsRenderer() {
        List<ProfilerDetailRenderer> list = new ArrayList<>(List.of(
            SymfonyProfilerRequestDetailRenderer.INSTANCE,
            SymfonyProfilerLoggerDetailRenderer.INSTANCE,
            SymfonyProfilerTranslationDetailRenderer.INSTANCE,
            SymfonyProfilerEventsDetailRenderer.INSTANCE,
            SymfonyProfilerTimeDetailRenderer.INSTANCE,
            SymfonyProfilerTwigDetailRenderer.INSTANCE,
            SymfonyProfilerDatabaseDetailRenderer.INSTANCE
        ));
        // stable sort, same as Kotlin sortedByDescending
        list.sort(Comparator.comparingInt(ProfilerDetailRenderer::getOverviewWeight).reversed());
        renderers = list;
    }

    public String render(SymfonyProfilerProfile profile, String requestedHash, @Nullable String collector, int page) {
        StringBuilder builder = new StringBuilder();
        appendRequestOverview(builder, profile, requestedHash);

        Set<String> availableCollectorNames = Set.copyOf(profile.getCollectorNames());
        if (collector != null) {
            ProfilerDetailRenderer renderer = renderers.stream()
                .filter(it -> it.getName().equals(collector))
                .findFirst()
                .orElse(null);
            if (renderer != null) {
                appendSection(builder, render(renderer.getName(), () -> renderer.renderDetails(profile, Math.max(page, 1))));
            }
            else {
                appendSection(builder, render(collector, () ->
                    SymfonyProfilerFallbackDetailRenderer.INSTANCE.renderDetails(profile, collector, Math.max(page, 1))
                ));
            }
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        List<ProfilerDetailRenderer> availableRenderers = renderers.stream()
            .filter(it -> availableCollectorNames.contains(it.getName()))
            .toList();
        if (availableCollectorNames.isEmpty()) {
            appendSection(builder, "No profiler collectors are available for this request.");
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        for (ProfilerDetailRenderer renderer : availableRenderers) {
            String overview = render(renderer.getName(), () -> renderer.renderOverview(profile));
            if (overview == null) {
                continue;
            }
            appendSection(builder, overview);
        }

        List<String> specializedNames = profile.getCollectorNames().stream()
            .filter(name -> renderers.stream().anyMatch(it -> it.getName().equals(name)))
            .toList();
        List<String> fallbackNames = profile.getCollectorNames().stream()
            .filter(name -> renderers.stream().noneMatch(it -> it.getName().equals(name)))
            .toList();

        StringBuilder collectors = new StringBuilder();
        collectors.append("## Available collectors").append('\n');
        collectors.append('\n');
        if (!specializedNames.isEmpty()) {
            collectors.append("- Specialized: ")
                .append(specializedNames.stream().map(SymfonyProfilerRequestDetailsRenderer::plainText).collect(Collectors.joining(", ")))
                .append('\n');
        }
        if (!fallbackNames.isEmpty()) {
            collectors.append("- Additional collectors: ")
                .append(fallbackNames.stream().map(SymfonyProfilerRequestDetailsRenderer::plainText).collect(Collectors.joining(", ")))
                .append('\n');
        }
        appendSection(builder, ProfilerDetailRendererKt.trimEnd(collectors));

        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Kotlin default arguments {@code collector = null, page = 1}.
     */
    public String render(SymfonyProfilerProfile profile, String requestedHash) {
        return render(profile, requestedHash, null, 1);
    }

    /**
     * Kotlin default argument {@code page = 1}.
     */
    public String render(SymfonyProfilerProfile profile, String requestedHash, @Nullable String collector) {
        return render(profile, requestedHash, collector, 1);
    }

    private static <T> T render(String collectorName, Supplier<T> render) {
        try {
            return render.get();
        }
        catch (Exception exception) {
            throw new ProfilerRendererException(collectorName, exception);
        }
    }

    /**
     * Appends one Markdown section with exactly one blank line after the preceding section.
     */
    private static void appendSection(StringBuilder builder, String section) {
        if (builder.length() > 0) {
            if (!endsWith(builder, "\n")) {
                builder.append('\n');
            }
            if (!endsWith(builder, "\n\n")) {
                builder.append('\n');
            }
        }
        builder.append(section);
    }

    private static boolean endsWith(StringBuilder builder, String suffix) {
        int length = builder.length();
        return length >= suffix.length() && builder.substring(length - suffix.length()).equals(suffix);
    }

    private static void appendRequestOverview(StringBuilder builder, SymfonyProfilerProfile profile, String requestedHash) {
        String token = profile.getToken();
        builder.append("Symfony Profiler Request - ").append(plainText(token != null ? token : requestedHash));

        Long time = profile.getTime();
        String formattedTime = time != null ? ProfilerTextRenderer.INSTANCE.formatTimestamp(time) : null;
        if (formattedTime != null) {
            builder.append(" (").append(formattedTime).append(")");
        }

        String url = profile.getUrl();
        if (url != null) {
            builder.append(" ").append(plainText(url));
        }

        Integer statusCode = profile.getStatusCode();
        if (statusCode != null) {
            builder.append(" ").append(statusCode);
        }

        String method = profile.getMethod();
        if (method != null) {
            builder.append(" ").append(plainText(method));
        }
        builder.append('\n');
    }

    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
