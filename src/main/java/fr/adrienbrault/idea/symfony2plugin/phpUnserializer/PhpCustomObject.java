package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A legacy {@code Serializable} ({@code C}) value whose payload is intentionally opaque.
 */
public final class PhpCustomObject implements PhpValue {
    private final PhpBytes myClassName;
    private final PhpBytes myPayload;

    public PhpCustomObject(PhpBytes className, PhpBytes payload) {
        myClassName = className;
        myPayload = payload;
    }

    public PhpBytes getClassName() {
        return myClassName;
    }

    public PhpBytes getPayload() {
        return myPayload;
    }

    public PhpBytes component1() {
        return myClassName;
    }

    public PhpBytes component2() {
        return myPayload;
    }

    public PhpCustomObject copy(PhpBytes className, PhpBytes payload) {
        return new PhpCustomObject(className, payload);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpCustomObject that)) {
            return false;
        }
        return Objects.equals(myClassName, that.myClassName) && Objects.equals(myPayload, that.myPayload);
    }

    @Override
    public int hashCode() {
        int result = myClassName.hashCode();
        result = result * 31 + myPayload.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpCustomObject(className=" + myClassName + ", payload=" + myPayload + ")";
    }
}
