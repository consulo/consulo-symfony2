package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslation;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTranslationMessage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Renders Symfony translation counts and a paginated CSV message list.
 */
public final class SymfonyProfilerTranslationDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerTranslationDetailRenderer INSTANCE = new SymfonyProfilerTranslationDetailRenderer();

    private static final int MAX_CSV_VALUE_LENGTH = 1_000;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerTranslationDetailRenderer() {
    }

    @Override
    public String getName() {
        return "translation";
    }

    @Override
    public int getOverviewWeight() {
        return 80;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerTranslationConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerTranslationConsumer.INSTANCE.read(profile), page);
    }

    /**
     * Mirrors the stable metrics from Symfony's translation profiler panel.
     */
    public String formatOverview(SymfonyProfilerTranslation translation) {
        StringBuilder builder = new StringBuilder();
        appendSummary(builder, translation);
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Renders actionable states first and keeps every escaped CSV row on one detail page.
     */
    public String formatDetails(SymfonyProfilerTranslation translation, int page) {
        List<SymfonyProfilerTranslationMessage> messages = translation.getMessages();
        List<String> rows = IntStream.range(0, messages.size())
            .boxed()
            .sorted(
                Comparator.<Integer>comparingInt(index -> messages.get(index).getState().getDisplayOrder())
                    .thenComparingInt(index -> index)
            )
            .map(index -> formatCsvRow(messages.get(index)))
            .toList();
        ProfilerDetailPage detailPage = ProfilerDetailRendererKt.paginateProfilerDetailEntries(rows, page);
        String heading;
        if (detailPage.isPaginated()) {
            heading = "### Messages (CSV, page " + detailPage.getNumber() + " of " + detailPage.getTotal() + ")";
        }
        else {
            heading = "### Messages (CSV)";
        }

        StringBuilder builder = new StringBuilder();
        appendSummary(builder, translation);
        builder.append('\n');
        builder.append(heading).append('\n');
        builder.append('\n');
        if (detailPage.getEntries().isEmpty()) {
            builder.append("No translation messages recorded.").append('\n');
        }
        else {
            builder.append("state,locale,fallback_locale,domain,count,id,translation").append('\n');
            for (String row : detailPage.getEntries()) {
                builder.append(row).append('\n');
            }
        }
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    /**
     * Kotlin default argument {@code page = 1}.
     */
    public String formatDetails(SymfonyProfilerTranslation translation) {
        return formatDetails(translation, 1);
    }

    private static void appendSummary(StringBuilder builder, SymfonyProfilerTranslation translation) {
        String locale = translation.getLocale();
        builder.append("## Collector: translation").append('\n');
        builder.append('\n');
        builder.append("- Default locale: ").append(plainText(locale != null ? locale : "none")).append('\n');
        builder.append(
            "- Fallback locales: " +
                (translation.getFallbackLocales().isEmpty()
                    ? "none"
                    : translation.getFallbackLocales().stream()
                        .map(SymfonyProfilerTranslationDetailRenderer::plainText)
                        .collect(Collectors.joining(", ")))
        ).append('\n');
        builder.append("- Defined messages: ").append(translation.getDefinedCount()).append('\n');
        builder.append("- Missing messages: ").append(translation.getMissingCount()).append('\n');
        builder.append("- Fallback messages: ").append(translation.getFallbackCount()).append('\n');
    }

    private static String formatCsvRow(SymfonyProfilerTranslationMessage message) {
        List<String> values = new ArrayList<>();
        values.add(message.getState().getCsvValue());
        values.add(orEmpty(message.getLocale()));
        values.add(orEmpty(message.getFallbackLocale()));
        values.add(orEmpty(message.getDomain()));
        values.add(String.valueOf(message.getCount()));
        values.add(orEmpty(message.getId()));
        values.add(orEmpty(message.getTranslation()));
        return values.stream().map(SymfonyProfilerTranslationDetailRenderer::csvValue).collect(Collectors.joining(","));
    }

    private static String orEmpty(@Nullable String value) {
        return value != null ? value : "";
    }

    /**
     * Produces bounded single-line RFC 4180 fields for an untrusted profiler value.
     */
    private static String csvValue(String value) {
        String normalized = CONTROL_CHARACTERS.matcher(value).replaceAll(" ");
        String bounded;
        if (normalized.length() <= MAX_CSV_VALUE_LENGTH) {
            bounded = normalized;
        }
        else {
            bounded = normalized.substring(0, MAX_CSV_VALUE_LENGTH) + "… [truncated]";
        }
        if (bounded.indexOf(',') >= 0 || bounded.indexOf('"') >= 0) {
            return "\"" + bounded.replace("\"", "\"\"") + "\"";
        }
        else {
            return bounded;
        }
    }

    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
