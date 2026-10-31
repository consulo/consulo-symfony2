package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

/** Represents a recursive VarDumper position without constructing a cyclic Kotlin graph. */
public final class ProfilerReference implements ProfilerValue {
    private final long myPosition;

    public ProfilerReference(long position) {
        myPosition = position;
    }

    public long getPosition() {
        return myPosition;
    }

    public long component1() {
        return myPosition;
    }

    public ProfilerReference copy(long position) {
        return new ProfilerReference(position);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerReference that)) {
            return false;
        }
        return myPosition == that.myPosition;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(myPosition);
    }

    @Override
    public String toString() {
        return "ProfilerReference(position=" + myPosition + ")";
    }
}
