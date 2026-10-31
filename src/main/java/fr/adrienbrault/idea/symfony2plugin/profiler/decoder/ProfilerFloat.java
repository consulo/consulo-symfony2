package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

public final class ProfilerFloat implements ProfilerValue {
    private final double myValue;

    public ProfilerFloat(double value) {
        myValue = value;
    }

    public double getValue() {
        return myValue;
    }

    public double component1() {
        return myValue;
    }

    public ProfilerFloat copy(double value) {
        return new ProfilerFloat(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerFloat that)) {
            return false;
        }
        return Double.compare(myValue, that.myValue) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(myValue);
    }

    @Override
    public String toString() {
        return "ProfilerFloat(value=" + myValue + ")";
    }
}
