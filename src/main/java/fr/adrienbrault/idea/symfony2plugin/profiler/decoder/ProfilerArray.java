package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Keeps PHP array order and mixed integer/string keys without coercing them into a Kotlin map. */
public final class ProfilerArray implements ProfilerValue {
    private final List<ProfilerEntry> myEntries;

    public ProfilerArray(List<ProfilerEntry> entries) {
        myEntries = entries;
    }

    public List<ProfilerEntry> getEntries() {
        return myEntries;
    }

    public @Nullable ProfilerValue get(String key) {
        PhpBytes bytes = new PhpBytes(key.getBytes(StandardCharsets.UTF_8));
        for (ProfilerEntry entry : myEntries) {
            if (entry.getKey() instanceof PhpStringKey stringKey && stringKey.getBytes().equals(bytes)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<ProfilerEntry> component1() {
        return myEntries;
    }

    public ProfilerArray copy(List<ProfilerEntry> entries) {
        return new ProfilerArray(entries);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerArray that)) {
            return false;
        }
        return myEntries.equals(that.myEntries);
    }

    @Override
    public int hashCode() {
        return myEntries.hashCode();
    }

    @Override
    public String toString() {
        return "ProfilerArray(entries=" + myEntries + ")";
    }
}
