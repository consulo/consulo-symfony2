package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes;
import org.jspecify.annotations.Nullable;

public final class ProfilerString implements ProfilerValue {
    private final PhpBytes myBytes;

    public ProfilerString(PhpBytes bytes) {
        myBytes = bytes;
    }

    public PhpBytes getBytes() {
        return myBytes;
    }

    public @Nullable String utf8StringOrNull() {
        return myBytes.utf8StringOrNull();
    }

    public PhpBytes component1() {
        return myBytes;
    }

    public ProfilerString copy(PhpBytes bytes) {
        return new ProfilerString(bytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerString that)) {
            return false;
        }
        return myBytes.equals(that.myBytes);
    }

    @Override
    public int hashCode() {
        return myBytes.hashCode();
    }

    @Override
    public String toString() {
        return "ProfilerString(bytes=" + myBytes + ")";
    }
}
