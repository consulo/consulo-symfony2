package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class SymfonyProfilerTime {
    private final double myDurationMs;
    private final double myInitializationTimeMs;
    private final @Nullable SymfonyProfilerMemory myMemory;
    private final boolean myStopwatchInstalled;
    private final List<SymfonyProfilerTimeEvent> myEvents;

    public SymfonyProfilerTime(double durationMs, double initializationTimeMs, @Nullable SymfonyProfilerMemory memory, boolean stopwatchInstalled, List<SymfonyProfilerTimeEvent> events) {
        myDurationMs = durationMs;
        myInitializationTimeMs = initializationTimeMs;
        myMemory = memory;
        myStopwatchInstalled = stopwatchInstalled;
        myEvents = events;
    }

    public double getDurationMs() {
        return myDurationMs;
    }

    public double getInitializationTimeMs() {
        return myInitializationTimeMs;
    }

    public @Nullable SymfonyProfilerMemory getMemory() {
        return myMemory;
    }

    public boolean getStopwatchInstalled() {
        return myStopwatchInstalled;
    }

    public List<SymfonyProfilerTimeEvent> getEvents() {
        return myEvents;
    }

    public double component1() {
        return myDurationMs;
    }

    public double component2() {
        return myInitializationTimeMs;
    }

    public @Nullable SymfonyProfilerMemory component3() {
        return myMemory;
    }

    public boolean component4() {
        return myStopwatchInstalled;
    }

    public List<SymfonyProfilerTimeEvent> component5() {
        return myEvents;
    }

    public SymfonyProfilerTime copy(double durationMs, double initializationTimeMs, @Nullable SymfonyProfilerMemory memory, boolean stopwatchInstalled, List<SymfonyProfilerTimeEvent> events) {
        return new SymfonyProfilerTime(durationMs, initializationTimeMs, memory, stopwatchInstalled, events);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTime that)) {
            return false;
        }
        return Double.compare(myDurationMs, that.myDurationMs) == 0
            && Double.compare(myInitializationTimeMs, that.myInitializationTimeMs) == 0
            && Objects.equals(myMemory, that.myMemory)
            && myStopwatchInstalled == that.myStopwatchInstalled
            && myEvents.equals(that.myEvents);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(myDurationMs);
        result = 31 * result + Double.hashCode(myInitializationTimeMs);
        result = 31 * result + (myMemory == null ? 0 : myMemory.hashCode());
        result = 31 * result + Boolean.hashCode(myStopwatchInstalled);
        result = 31 * result + myEvents.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTime(durationMs=" + myDurationMs + ", initializationTimeMs=" + myInitializationTimeMs + ", memory=" + myMemory + ", stopwatchInstalled=" + myStopwatchInstalled + ", events=" + myEvents + ")";
    }
}
