package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

public final class PhpIntegerKey implements PhpArrayKey {
    private final long myValue;

    public PhpIntegerKey(long value) {
        myValue = value;
    }

    public long getValue() {
        return myValue;
    }

    public long component1() {
        return myValue;
    }

    public PhpIntegerKey copy(long value) {
        return new PhpIntegerKey(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpIntegerKey that)) {
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
        return "PhpIntegerKey(value=" + myValue + ")";
    }
}
