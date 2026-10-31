package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

public final class PhpFloat implements PhpValue {
    private final double myValue;

    public PhpFloat(double value) {
        myValue = value;
    }

    public double getValue() {
        return myValue;
    }

    public double component1() {
        return myValue;
    }

    public PhpFloat copy(double value) {
        return new PhpFloat(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpFloat that)) {
            return false;
        }
        return Double.compare(myValue, that.myValue) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(myValue);
        return result;
    }

    @Override
    public String toString() {
        return "PhpFloat";
    }
}
