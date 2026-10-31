package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerEvents {
    private final List<SymfonyProfilerEventDispatcher> myDispatchers;

    public SymfonyProfilerEvents(List<SymfonyProfilerEventDispatcher> dispatchers) {
        myDispatchers = dispatchers;
    }

    public List<SymfonyProfilerEventDispatcher> getDispatchers() {
        return myDispatchers;
    }

    public int getDispatchedEventCount() {
        int count = 0;
        for (SymfonyProfilerEventDispatcher dispatcher : myDispatchers) {
            count += dispatcher.getEvents().size();
        }
        return count;
    }

    public int getCalledListenerCount() {
        int count = 0;
        for (SymfonyProfilerEventDispatcher dispatcher : myDispatchers) {
            for (SymfonyProfilerDispatchedEvent event : dispatcher.getEvents()) {
                count += event.getListeners().size();
            }
        }
        return count;
    }

    public List<SymfonyProfilerEventDispatcher> component1() {
        return myDispatchers;
    }

    public SymfonyProfilerEvents copy(List<SymfonyProfilerEventDispatcher> dispatchers) {
        return new SymfonyProfilerEvents(dispatchers);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerEvents that)) {
            return false;
        }
        return myDispatchers.equals(that.myDispatchers);
    }

    @Override
    public int hashCode() {
        return myDispatchers.hashCode();
    }

    @Override
    public String toString() {
        return "SymfonyProfilerEvents(dispatchers=" + myDispatchers + ")";
    }
}
