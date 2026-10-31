package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

public final class PhpBoolean implements PhpValue {
    private final boolean myValue;

    public PhpBoolean(boolean value) {
        myValue = value;
    }

    public boolean getValue() {
        return myValue;
    }

    public boolean component1() {
        return myValue;
    }

    public PhpBoolean copy(boolean value) {
        return new PhpBoolean(value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpBoolean that)) {
            return false;
        }
        return myValue == that.myValue;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(myValue);
        return result;
    }

    @Override
    public String toString() {
        return "PhpBoolean";
    }
}
