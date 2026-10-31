package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

/**
 * Top-level functions of upstream {@code PhpValue.kt}.
 */
public final class PhpValueKt {
    private PhpValueKt() {
    }

    public static PhpArray requireArray(PhpValue value) {
        if (value instanceof PhpArray phpArray) {
            return phpArray;
        }
        throw new IllegalStateException("Expected PhpArray, got " + value.getClass().getSimpleName());
    }

    public static PhpObject requireObject(PhpValue value) {
        if (value instanceof PhpObject phpObject) {
            return phpObject;
        }
        throw new IllegalStateException("Expected PhpObject, got " + value.getClass().getSimpleName());
    }

    public static PhpReference requireReference(PhpValue value) {
        if (value instanceof PhpReference phpReference) {
            return phpReference;
        }
        throw new IllegalStateException("Expected PhpReference, got " + value.getClass().getSimpleName());
    }

    public static @Nullable String utf8StringOrNull(PhpValue value) {
        return value instanceof PhpString phpString ? phpString.getBytes().utf8StringOrNull() : null;
    }

    public static PhpObjectProperty createPhpObjectProperty(PhpArrayKey key, PhpValue value) {
        PhpBytes name = key instanceof PhpStringKey stringKey ? stringKey.getBytes() : null;
        if (name == null) {
            return new PhpObjectProperty(key, value, null, null, null);
        }

        if (name.getSize() >= 3 && name.byteAt(0) == (byte) 0) {
            int separator = name.indexOf((byte) 0, 1);
            if (separator > 1) {
                PhpBytes scope = name.slice(1, separator - 1);
                PhpBytes logicalName = name.slice(separator + 1, name.getSize() - separator - 1);
                if (scope.getSize() == 1 && scope.byteAt(0) == (byte) '*') {
                    return new PhpObjectProperty(
                        key,
                        value,
                        PhpPropertyVisibility.PROTECTED,
                        null,
                        logicalName
                    );
                }

                return new PhpObjectProperty(
                    key,
                    value,
                    PhpPropertyVisibility.PRIVATE,
                    scope,
                    logicalName
                );
            }
        }

        return new PhpObjectProperty(
            key,
            value,
            PhpPropertyVisibility.PUBLIC,
            null,
            name
        );
    }
}
