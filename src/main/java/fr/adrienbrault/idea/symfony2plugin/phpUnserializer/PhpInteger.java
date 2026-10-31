package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

public final class PhpInteger implements PhpValue {
    private final long myValue;

    public PhpInteger(long value) {
        myValue = value;
    }

    public long getValue() {
        return myValue;
    }

    public long component1() {
        return myValue;
    }

    public PhpInteger copy(long value) {
        return new PhpInteger(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpInteger that)) {
            return false;
        }
        return myValue == that.myValue;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(myValue);
        return result;
    }

    @Override
    public String toString() {
        return "PhpInteger";
    }
}
