package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

public final class SymfonyProfilerMemory {
    private final long myPeakBytes;
    private final long myLimitBytes;

    public SymfonyProfilerMemory(long peakBytes, long limitBytes) {
        myPeakBytes = peakBytes;
        myLimitBytes = limitBytes;
    }

    public long getPeakBytes() {
        return myPeakBytes;
    }

    public long getLimitBytes() {
        return myLimitBytes;
    }

    public long component1() {
        return myPeakBytes;
    }

    public long component2() {
        return myLimitBytes;
    }

    public SymfonyProfilerMemory copy(long peakBytes, long limitBytes) {
        return new SymfonyProfilerMemory(peakBytes, limitBytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerMemory that)) {
            return false;
        }
        return myPeakBytes == that.myPeakBytes
            && myLimitBytes == that.myLimitBytes;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(myPeakBytes);
        result = 31 * result + Long.hashCode(myLimitBytes);
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerMemory(peakBytes=" + myPeakBytes + ", limitBytes=" + myLimitBytes + ")";
    }
}
