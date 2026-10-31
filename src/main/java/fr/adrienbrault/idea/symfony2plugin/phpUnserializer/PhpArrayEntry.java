package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class PhpArrayEntry {
    private final PhpArrayKey myKey;
    private final PhpValue myValue;

    public PhpArrayEntry(PhpArrayKey key, PhpValue value) {
        myKey = key;
        myValue = value;
    }

    public PhpArrayKey getKey() {
        return myKey;
    }

    public PhpValue getValue() {
        return myValue;
    }

    public PhpArrayKey component1() {
        return myKey;
    }

    public PhpValue component2() {
        return myValue;
    }

    public PhpArrayEntry copy(PhpArrayKey key, PhpValue value) {
        return new PhpArrayEntry(key, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpArrayEntry that)) {
            return false;
        }
        return Objects.equals(myKey, that.myKey) && Objects.equals(myValue, that.myValue);
    }

    @Override
    public int hashCode() {
        int result = myKey.hashCode();
        result = result * 31 + myValue.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpArrayEntry(key=" + myKey + ", value=" + myValue.getClass().getSimpleName() + ")";
    }
}
