package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class PhpObjectProperty {
    private final PhpArrayKey myKey;
    private final PhpValue myValue;
    private final @Nullable PhpPropertyVisibility myVisibility;
    private final @Nullable PhpBytes myDeclaringClass;
    private final @Nullable PhpBytes myLogicalName;

    public PhpObjectProperty(PhpArrayKey key, PhpValue value, @Nullable PhpPropertyVisibility visibility, @Nullable PhpBytes declaringClass, @Nullable PhpBytes logicalName) {
        myKey = key;
        myValue = value;
        myVisibility = visibility;
        myDeclaringClass = declaringClass;
        myLogicalName = logicalName;
    }

    public PhpArrayKey getKey() {
        return myKey;
    }

    public PhpValue getValue() {
        return myValue;
    }

    public @Nullable PhpPropertyVisibility getVisibility() {
        return myVisibility;
    }

    public @Nullable PhpBytes getDeclaringClass() {
        return myDeclaringClass;
    }

    public @Nullable PhpBytes getLogicalName() {
        return myLogicalName;
    }

    /**
     * Raw property name, or {@code null} for an integer key produced by {@code __serialize()}.
     */
    public @Nullable PhpBytes getName() {
        return myKey instanceof PhpStringKey stringKey ? stringKey.getBytes() : null;
    }

    public PhpArrayKey component1() {
        return myKey;
    }

    public PhpValue component2() {
        return myValue;
    }

    public @Nullable PhpPropertyVisibility component3() {
        return myVisibility;
    }

    public @Nullable PhpBytes component4() {
        return myDeclaringClass;
    }

    public @Nullable PhpBytes component5() {
        return myLogicalName;
    }

    public PhpObjectProperty copy(PhpArrayKey key, PhpValue value, @Nullable PhpPropertyVisibility visibility, @Nullable PhpBytes declaringClass, @Nullable PhpBytes logicalName) {
        return new PhpObjectProperty(key, value, visibility, declaringClass, logicalName);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpObjectProperty that)) {
            return false;
        }
        return Objects.equals(myKey, that.myKey) && Objects.equals(myValue, that.myValue) && Objects.equals(myVisibility, that.myVisibility) && Objects.equals(myDeclaringClass, that.myDeclaringClass) && Objects.equals(myLogicalName, that.myLogicalName);
    }

    @Override
    public int hashCode() {
        int result = myKey.hashCode();
        result = result * 31 + myValue.hashCode();
        result = result * 31 + (myVisibility == null ? 0 : myVisibility.hashCode());
        result = result * 31 + (myDeclaringClass == null ? 0 : myDeclaringClass.hashCode());
        result = result * 31 + (myLogicalName == null ? 0 : myLogicalName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PhpObjectProperty(key=" + myKey + ", visibility=" + myVisibility + ", declaringClass=" + myDeclaringClass + ", logicalName=" + myLogicalName + ", value=" + myValue.getClass().getSimpleName() + ")";
    }
}
