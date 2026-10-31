package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An unloaded PHP object with its raw class name and ordered properties.
 */
public final class PhpObject implements PhpValue {
    private final PhpBytes myClassName;
    private final List<PhpObjectProperty> myProperties;

    private volatile @Nullable Map<PhpArrayKey, PhpValue> myLookupIndex;

    public PhpObject(PhpBytes className, List<PhpObjectProperty> properties) {
        myClassName = className;
        myProperties = properties;
    }

    public PhpBytes getClassName() {
        return myClassName;
    }

    public List<PhpObjectProperty> getProperties() {
        return myProperties;
    }

    private Map<PhpArrayKey, PhpValue> getLookupIndex() {
        Map<PhpArrayKey, PhpValue> lookupIndex = myLookupIndex;
        if (lookupIndex != null) {
            return lookupIndex;
        }

        synchronized (this) {
            lookupIndex = myLookupIndex;
            if (lookupIndex == null) {
                lookupIndex = new LinkedHashMap<>();
                for (PhpObjectProperty property : myProperties) {
                    PhpArrayKey key;
                    if (property.getKey() instanceof PhpIntegerKey) {
                        key = property.getKey();
                    }
                    else {
                        // PhpStringKey
                        PhpBytes logicalName = property.getLogicalName();
                        key = logicalName != null ? new PhpStringKey(logicalName) : null;
                    }
                    if (key != null) {
                        lookupIndex.putIfAbsent(key, property.getValue());
                    }
                }
                myLookupIndex = lookupIndex;
            }
            return lookupIndex;
        }
    }

    public @Nullable PhpValue get(String logicalName) {
        return getLookupIndex().get(new PhpStringKey(PhpBytes.Companion.utf8(logicalName)));
    }

    public @Nullable PhpValue get(long key) {
        return getLookupIndex().get(new PhpIntegerKey(key));
    }

    public PhpBytes component1() {
        return myClassName;
    }

    public List<PhpObjectProperty> component2() {
        return myProperties;
    }

    public PhpObject copy(PhpBytes className, List<PhpObjectProperty> properties) {
        return new PhpObject(className, properties);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpObject that)) {
            return false;
        }
        return Objects.equals(myClassName, that.myClassName) && Objects.equals(myProperties, that.myProperties);
    }

    @Override
    public int hashCode() {
        int result = myClassName.hashCode();
        result = result * 31 + myProperties.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpObject(className=" + myClassName + ", propertyCount=" + myProperties.size() + ")";
    }
}
