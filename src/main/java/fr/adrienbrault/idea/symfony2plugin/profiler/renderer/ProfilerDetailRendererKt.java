package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level declarations of {@code ProfilerDetailRenderer.kt}.
 */
public final class ProfilerDetailRendererKt {
    public static final int PROFILER_DETAIL_PAGE_TOKEN_TARGET = 4_000;
    public static final double APPROXIMATE_CHARACTERS_PER_TOKEN = 3.75;
    private static final int PROFILER_DETAIL_PAGE_CHARACTER_TARGET =
        (int) (PROFILER_DETAIL_PAGE_TOKEN_TARGET * APPROXIMATE_CHARACTERS_PER_TOKEN);

    private ProfilerDetailRendererKt() {
    }

    public static int getPROFILER_DETAIL_PAGE_CHARACTER_TARGET() {
        return PROFILER_DETAIL_PAGE_CHARACTER_TARGET;
    }

    /**
     * Paginates complete rendered entries around the approximate token target.
     * <p>
     * The entry crossing the character target remains on the current page. This makes the target soft
     * and avoids splitting a rendered value or database query between pages.
     */
    public static ProfilerDetailPage paginateProfilerDetailEntries(List<String> entries, int requestedPage, int characterTarget) {
        if (characterTarget <= 0) {
            throw new IllegalArgumentException("characterTarget must be greater than zero");
        }

        List<List<String>> pages = new ArrayList<>();
        List<String> currentEntries = new ArrayList<>();
        int currentCharacterCount = 0;

        for (String entry : entries) {
            currentEntries.add(entry);
            currentCharacterCount += entry.length() + 1;

            if (currentCharacterCount >= characterTarget) {
                pages.add(currentEntries);
                currentEntries = new ArrayList<>();
                currentCharacterCount = 0;
            }
        }

        if (!currentEntries.isEmpty() || pages.isEmpty()) {
            pages.add(currentEntries);
        }

        int currentPage = Math.min(Math.max(requestedPage, 1), pages.size());
        return new ProfilerDetailPage(pages.get(currentPage - 1), currentPage, pages.size());
    }

    /**
     * Kotlin default argument {@code characterTarget = PROFILER_DETAIL_PAGE_CHARACTER_TARGET}.
     */
    public static ProfilerDetailPage paginateProfilerDetailEntries(List<String> entries, int requestedPage) {
        return paginateProfilerDetailEntries(entries, requestedPage, PROFILER_DETAIL_PAGE_CHARACTER_TARGET);
    }

    /**
     * Kotlin {@code CharSequence.trimEnd()}: strips trailing characters matching {@code Char.isWhitespace()}.
     */
    static String trimEnd(CharSequence value) {
        int end = value.length();
        while (end > 0 && isKotlinWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.subSequence(0, end).toString();
    }

    /**
     * Kotlin {@code String.trim()}: strips leading and trailing characters matching {@code Char.isWhitespace()}.
     */
    static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isKotlinWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isKotlinWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isKotlinWhitespace(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
    }
}
