package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import org.jspecify.annotations.Nullable;

public final class SymfonyProfilerFallbackCollector {
    private final String myName;
    private final ProfilerValue myData;

    public SymfonyProfilerFallbackCollector(String name, ProfilerValue data) {
        myName = name;
        myData = data;
    }

    public String getName() {
        return myName;
    }

    public ProfilerValue getData() {
        return myData;
    }

    public String component1() {
        return myName;
    }

    public ProfilerValue component2() {
        return myData;
    }

    public SymfonyProfilerFallbackCollector copy(String name, ProfilerValue data) {
        return new SymfonyProfilerFallbackCollector(name, data);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerFallbackCollector that)) {
            return false;
        }
        return myName.equals(that.myName)
            && myData.equals(that.myData);
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + myData.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerFallbackCollector(name=" + myName + ", data=" + myData + ")";
    }
}
