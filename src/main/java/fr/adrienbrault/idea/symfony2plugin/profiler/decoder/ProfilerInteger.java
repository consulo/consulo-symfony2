package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

public final class ProfilerInteger implements ProfilerValue {
    private final long myValue;

    public ProfilerInteger(long value) {
        myValue = value;
    }

    public long getValue() {
        return myValue;
    }

    public long component1() {
        return myValue;
    }

    public ProfilerInteger copy(long value) {
        return new ProfilerInteger(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerInteger that)) {
            return false;
        }
        return myValue == that.myValue;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(myValue);
    }

    @Override
    public String toString() {
        return "ProfilerInteger(value=" + myValue + ")";
    }
}
