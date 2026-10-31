package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

public final class PhpNull implements PhpValue {
    public static final PhpNull INSTANCE = new PhpNull();

    private PhpNull() {
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof PhpNull;
    }

    @Override
    public int hashCode() {
        return "PhpNull".hashCode();
    }

    @Override
    public String toString() {
        return "PhpNull";
    }
}
