package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class PhpStringKey implements PhpArrayKey {
    private final PhpBytes myBytes;

    public PhpStringKey(PhpBytes bytes) {
        myBytes = bytes;
    }

    public PhpBytes getBytes() {
        return myBytes;
    }

    public PhpBytes component1() {
        return myBytes;
    }

    public PhpStringKey copy(PhpBytes bytes) {
        return new PhpStringKey(bytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpStringKey that)) {
            return false;
        }
        return Objects.equals(myBytes, that.myBytes);
    }

    @Override
    public int hashCode() {
        int result = myBytes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpStringKey(bytes=" + myBytes + ")";
    }
}
