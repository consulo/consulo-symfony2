package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import org.jspecify.annotations.Nullable;

public final class ProfilerNull implements ProfilerValue {
    public static final ProfilerNull INSTANCE = new ProfilerNull();

    private ProfilerNull() {
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof ProfilerNull;
    }

    @Override
    public int hashCode() {
        return "ProfilerNull".hashCode();
    }

    @Override
    public String toString() {
        return "ProfilerNull";
    }
}
