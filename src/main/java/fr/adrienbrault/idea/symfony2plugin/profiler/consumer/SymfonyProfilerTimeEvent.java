package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

public final class SymfonyProfilerTimeEvent {
    private final String myName;
    private final String myCategory;
    private final double myStartMs;
    private final double myEndMs;
    private final double myDurationMs;
    private final long myMemoryBytes;

    public SymfonyProfilerTimeEvent(String name, String category, double startMs, double endMs, double durationMs, long memoryBytes) {
        myName = name;
        myCategory = category;
        myStartMs = startMs;
        myEndMs = endMs;
        myDurationMs = durationMs;
        myMemoryBytes = memoryBytes;
    }

    public String getName() {
        return myName;
    }

    public String getCategory() {
        return myCategory;
    }

    public double getStartMs() {
        return myStartMs;
    }

    public double getEndMs() {
        return myEndMs;
    }

    public double getDurationMs() {
        return myDurationMs;
    }

    public long getMemoryBytes() {
        return myMemoryBytes;
    }

    public String component1() {
        return myName;
    }

    public String component2() {
        return myCategory;
    }

    public double component3() {
        return myStartMs;
    }

    public double component4() {
        return myEndMs;
    }

    public double component5() {
        return myDurationMs;
    }

    public long component6() {
        return myMemoryBytes;
    }

    public SymfonyProfilerTimeEvent copy(String name, String category, double startMs, double endMs, double durationMs, long memoryBytes) {
        return new SymfonyProfilerTimeEvent(name, category, startMs, endMs, durationMs, memoryBytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTimeEvent that)) {
            return false;
        }
        return myName.equals(that.myName)
            && myCategory.equals(that.myCategory)
            && Double.compare(myStartMs, that.myStartMs) == 0
            && Double.compare(myEndMs, that.myEndMs) == 0
            && Double.compare(myDurationMs, that.myDurationMs) == 0
            && myMemoryBytes == that.myMemoryBytes;
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + myCategory.hashCode();
        result = 31 * result + Double.hashCode(myStartMs);
        result = 31 * result + Double.hashCode(myEndMs);
        result = 31 * result + Double.hashCode(myDurationMs);
        result = 31 * result + Long.hashCode(myMemoryBytes);
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTimeEvent(name=" + myName + ", category=" + myCategory + ", startMs=" + myStartMs + ", endMs=" + myEndMs + ", durationMs=" + myDurationMs + ", memoryBytes=" + myMemoryBytes + ")";
    }
}
