package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class SymfonyProfilerTranslation {
    private final @Nullable String myLocale;
    private final List<String> myFallbackLocales;
    private final int myDefinedCount;
    private final int myMissingCount;
    private final int myFallbackCount;
    private final List<SymfonyProfilerTranslationMessage> myMessages;

    public SymfonyProfilerTranslation(@Nullable String locale, List<String> fallbackLocales, int definedCount, int missingCount, int fallbackCount, List<SymfonyProfilerTranslationMessage> messages) {
        myLocale = locale;
        myFallbackLocales = fallbackLocales;
        myDefinedCount = definedCount;
        myMissingCount = missingCount;
        myFallbackCount = fallbackCount;
        myMessages = messages;
    }

    public @Nullable String getLocale() {
        return myLocale;
    }

    public List<String> getFallbackLocales() {
        return myFallbackLocales;
    }

    public int getDefinedCount() {
        return myDefinedCount;
    }

    public int getMissingCount() {
        return myMissingCount;
    }

    public int getFallbackCount() {
        return myFallbackCount;
    }

    public List<SymfonyProfilerTranslationMessage> getMessages() {
        return myMessages;
    }

    public @Nullable String component1() {
        return myLocale;
    }

    public List<String> component2() {
        return myFallbackLocales;
    }

    public int component3() {
        return myDefinedCount;
    }

    public int component4() {
        return myMissingCount;
    }

    public int component5() {
        return myFallbackCount;
    }

    public List<SymfonyProfilerTranslationMessage> component6() {
        return myMessages;
    }

    public SymfonyProfilerTranslation copy(@Nullable String locale, List<String> fallbackLocales, int definedCount, int missingCount, int fallbackCount, List<SymfonyProfilerTranslationMessage> messages) {
        return new SymfonyProfilerTranslation(locale, fallbackLocales, definedCount, missingCount, fallbackCount, messages);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTranslation that)) {
            return false;
        }
        return Objects.equals(myLocale, that.myLocale)
            && myFallbackLocales.equals(that.myFallbackLocales)
            && myDefinedCount == that.myDefinedCount
            && myMissingCount == that.myMissingCount
            && myFallbackCount == that.myFallbackCount
            && myMessages.equals(that.myMessages);
    }

    @Override
    public int hashCode() {
        int result = (myLocale == null ? 0 : myLocale.hashCode());
        result = 31 * result + myFallbackLocales.hashCode();
        result = 31 * result + Integer.hashCode(myDefinedCount);
        result = 31 * result + Integer.hashCode(myMissingCount);
        result = 31 * result + Integer.hashCode(myFallbackCount);
        result = 31 * result + myMessages.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTranslation(locale=" + myLocale + ", fallbackLocales=" + myFallbackLocales + ", definedCount=" + myDefinedCount + ", missingCount=" + myMissingCount + ", fallbackCount=" + myFallbackCount + ", messages=" + myMessages + ")";
    }
}
