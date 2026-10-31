package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import org.jspecify.annotations.Nullable;

public final class SymfonyProfilerRequest {
    private final ProfilerArray myData;
    private final SymfonyProfilerRequestSummary mySummary;

    public SymfonyProfilerRequest(ProfilerArray data, SymfonyProfilerRequestSummary summary) {
        myData = data;
        mySummary = summary;
    }

    public ProfilerArray getData() {
        return myData;
    }

    public SymfonyProfilerRequestSummary getSummary() {
        return mySummary;
    }

    public ProfilerArray component1() {
        return myData;
    }

    public SymfonyProfilerRequestSummary component2() {
        return mySummary;
    }

    public SymfonyProfilerRequest copy(ProfilerArray data, SymfonyProfilerRequestSummary summary) {
        return new SymfonyProfilerRequest(data, summary);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerRequest that)) {
            return false;
        }
        return myData.equals(that.myData)
            && mySummary.equals(that.mySummary);
    }

    @Override
    public int hashCode() {
        int result = myData.hashCode();
        result = 31 * result + mySummary.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerRequest(data=" + myData + ", summary=" + mySummary + ")";
    }
}
