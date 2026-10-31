package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

public final class ProfilerBoolean implements ProfilerValue {
    private final boolean myValue;

    public ProfilerBoolean(boolean value) {
        myValue = value;
    }

    public boolean getValue() {
        return myValue;
    }

    public boolean component1() {
        return myValue;
    }

    public ProfilerBoolean copy(boolean value) {
        return new ProfilerBoolean(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerBoolean that)) {
            return false;
        }
        return myValue == that.myValue;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(myValue);
    }

    @Override
    public String toString() {
        return "ProfilerBoolean(value=" + myValue + ")";
    }
}
