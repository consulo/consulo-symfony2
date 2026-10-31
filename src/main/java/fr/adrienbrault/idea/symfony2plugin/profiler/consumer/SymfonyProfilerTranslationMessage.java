package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerTranslationMessage {
    private final @Nullable String myLocale;
    private final @Nullable String myFallbackLocale;
    private final @Nullable String myDomain;
    private final @Nullable String myId;
    private final @Nullable String myTranslation;
    private final SymfonyProfilerTranslationState myState;
    private final long myCount;

    public SymfonyProfilerTranslationMessage(@Nullable String locale, @Nullable String fallbackLocale, @Nullable String domain, @Nullable String id, @Nullable String translation, SymfonyProfilerTranslationState state, long count) {
        myLocale = locale;
        myFallbackLocale = fallbackLocale;
        myDomain = domain;
        myId = id;
        myTranslation = translation;
        myState = state;
        myCount = count;
    }

    public @Nullable String getLocale() {
        return myLocale;
    }

    public @Nullable String getFallbackLocale() {
        return myFallbackLocale;
    }

    public @Nullable String getDomain() {
        return myDomain;
    }

    public @Nullable String getId() {
        return myId;
    }

    public @Nullable String getTranslation() {
        return myTranslation;
    }

    public SymfonyProfilerTranslationState getState() {
        return myState;
    }

    public long getCount() {
        return myCount;
    }

    public @Nullable String component1() {
        return myLocale;
    }

    public @Nullable String component2() {
        return myFallbackLocale;
    }

    public @Nullable String component3() {
        return myDomain;
    }

    public @Nullable String component4() {
        return myId;
    }

    public @Nullable String component5() {
        return myTranslation;
    }

    public SymfonyProfilerTranslationState component6() {
        return myState;
    }

    public long component7() {
        return myCount;
    }

    public SymfonyProfilerTranslationMessage copy(@Nullable String locale, @Nullable String fallbackLocale, @Nullable String domain, @Nullable String id, @Nullable String translation, SymfonyProfilerTranslationState state, long count) {
        return new SymfonyProfilerTranslationMessage(locale, fallbackLocale, domain, id, translation, state, count);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTranslationMessage that)) {
            return false;
        }
        return Objects.equals(myLocale, that.myLocale)
            && Objects.equals(myFallbackLocale, that.myFallbackLocale)
            && Objects.equals(myDomain, that.myDomain)
            && Objects.equals(myId, that.myId)
            && Objects.equals(myTranslation, that.myTranslation)
            && myState.equals(that.myState)
            && myCount == that.myCount;
    }

    @Override
    public int hashCode() {
        int result = (myLocale == null ? 0 : myLocale.hashCode());
        result = 31 * result + (myFallbackLocale == null ? 0 : myFallbackLocale.hashCode());
        result = 31 * result + (myDomain == null ? 0 : myDomain.hashCode());
        result = 31 * result + (myId == null ? 0 : myId.hashCode());
        result = 31 * result + (myTranslation == null ? 0 : myTranslation.hashCode());
        result = 31 * result + myState.hashCode();
        result = 31 * result + Long.hashCode(myCount);
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTranslationMessage(locale=" + myLocale + ", fallbackLocale=" + myFallbackLocale + ", domain=" + myDomain + ", id=" + myId + ", translation=" + myTranslation + ", state=" + myState + ", count=" + myCount + ")";
    }
}
