package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerDispatchedEvent {
    private final String myName;
    private final List<SymfonyProfilerEventListener> myListeners;

    public SymfonyProfilerDispatchedEvent(String name, List<SymfonyProfilerEventListener> listeners) {
        myName = name;
        myListeners = listeners;
    }

    public String getName() {
        return myName;
    }

    public List<SymfonyProfilerEventListener> getListeners() {
        return myListeners;
    }

    public String component1() {
        return myName;
    }

    public List<SymfonyProfilerEventListener> component2() {
        return myListeners;
    }

    public SymfonyProfilerDispatchedEvent copy(String name, List<SymfonyProfilerEventListener> listeners) {
        return new SymfonyProfilerDispatchedEvent(name, listeners);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerDispatchedEvent that)) {
            return false;
        }
        return myName.equals(that.myName)
            && myListeners.equals(that.myListeners);
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + myListeners.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerDispatchedEvent(name=" + myName + ", listeners=" + myListeners + ")";
    }
}
