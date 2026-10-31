package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Opaque serialized payloads are described but never copied into profiler output. */
public final class ProfilerOpaque implements ProfilerValue {
    private final String myKind;
    private final @Nullable Integer myPayloadBytes;

    public ProfilerOpaque(String kind, @Nullable Integer payloadBytes) {
        myKind = kind;
        myPayloadBytes = payloadBytes;
    }

    public String getKind() {
        return myKind;
    }

    public @Nullable Integer getPayloadBytes() {
        return myPayloadBytes;
    }

    public String component1() {
        return myKind;
    }

    public @Nullable Integer component2() {
        return myPayloadBytes;
    }

    public ProfilerOpaque copy(String kind, @Nullable Integer payloadBytes) {
        return new ProfilerOpaque(kind, payloadBytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerOpaque that)) {
            return false;
        }
        return myKind.equals(that.myKind)
            && Objects.equals(myPayloadBytes, that.myPayloadBytes);
    }

    @Override
    public int hashCode() {
        int result = myKind.hashCode();
        result = 31 * result + (myPayloadBytes == null ? 0 : myPayloadBytes.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ProfilerOpaque(kind=" + myKind + ", payloadBytes=" + myPayloadBytes + ")";
    }
}
