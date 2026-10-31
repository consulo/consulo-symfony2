package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerEventDispatcher {
    private final String myName;
    private final List<SymfonyProfilerDispatchedEvent> myEvents;

    public SymfonyProfilerEventDispatcher(String name, List<SymfonyProfilerDispatchedEvent> events) {
        myName = name;
        myEvents = events;
    }

    public String getName() {
        return myName;
    }

    public List<SymfonyProfilerDispatchedEvent> getEvents() {
        return myEvents;
    }

    public String component1() {
        return myName;
    }

    public List<SymfonyProfilerDispatchedEvent> component2() {
        return myEvents;
    }

    public SymfonyProfilerEventDispatcher copy(String name, List<SymfonyProfilerDispatchedEvent> events) {
        return new SymfonyProfilerEventDispatcher(name, events);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerEventDispatcher that)) {
            return false;
        }
        return myName.equals(that.myName)
            && myEvents.equals(that.myEvents);
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + myEvents.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerEventDispatcher(name=" + myName + ", events=" + myEvents + ")";
    }
}
