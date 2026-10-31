package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered PHP array entries; integer and byte-string keys may be mixed.
 */
public final class PhpArray implements PhpValue {
    private final List<PhpArrayEntry> myEntries;

    private volatile @Nullable Map<PhpArrayKey, PhpValue> myLookupIndex;

    public PhpArray(List<PhpArrayEntry> entries) {
        myEntries = entries;
    }

    public List<PhpArrayEntry> getEntries() {
        return myEntries;
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
                for (PhpArrayEntry entry : myEntries) {
                    lookupIndex.putIfAbsent(entry.getKey(), entry.getValue());
                }
                myLookupIndex = lookupIndex;
            }
            return lookupIndex;
        }
    }

    public @Nullable PhpValue get(long key) {
        return getLookupIndex().get(new PhpIntegerKey(key));
    }

    public @Nullable PhpValue get(String key) {
        return getLookupIndex().get(new PhpStringKey(PhpBytes.Companion.utf8(key)));
    }

    public @Nullable PhpValue get(PhpBytes key) {
        return getLookupIndex().get(new PhpStringKey(key));
    }

    public List<PhpArrayEntry> component1() {
        return myEntries;
    }

    public PhpArray copy(List<PhpArrayEntry> entries) {
        return new PhpArray(entries);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpArray that)) {
            return false;
        }
        return Objects.equals(myEntries, that.myEntries);
    }

    @Override
    public int hashCode() {
        return myEntries.hashCode();
    }

    @Override
    public String toString() {
        return "PhpArray(entryCount=" + myEntries.size() + ")";
    }
}
