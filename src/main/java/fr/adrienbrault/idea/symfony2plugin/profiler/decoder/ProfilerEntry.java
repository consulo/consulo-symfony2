package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey;
import org.jspecify.annotations.Nullable;

public final class ProfilerEntry {
    private final PhpArrayKey myKey;
    private final ProfilerValue myValue;

    public ProfilerEntry(PhpArrayKey key, ProfilerValue value) {
        myKey = key;
        myValue = value;
    }

    public PhpArrayKey getKey() {
        return myKey;
    }

    public ProfilerValue getValue() {
        return myValue;
    }

    public PhpArrayKey component1() {
        return myKey;
    }

    public ProfilerValue component2() {
        return myValue;
    }

    public ProfilerEntry copy(PhpArrayKey key, ProfilerValue value) {
        return new ProfilerEntry(key, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerEntry that)) {
            return false;
        }
        return myKey.equals(that.myKey)
            && myValue.equals(that.myValue);
    }

    @Override
    public int hashCode() {
        int result = myKey.hashCode();
        result = 31 * result + myValue.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProfilerEntry(key=" + myKey + ", value=" + myValue + ")";
    }
}
