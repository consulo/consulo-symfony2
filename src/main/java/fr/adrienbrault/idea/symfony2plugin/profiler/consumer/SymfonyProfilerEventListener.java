package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerEventListener {
    private final @Nullable Integer myPriority;
    private final String myListener;

    public SymfonyProfilerEventListener(@Nullable Integer priority, String listener) {
        myPriority = priority;
        myListener = listener;
    }

    public @Nullable Integer getPriority() {
        return myPriority;
    }

    public String getListener() {
        return myListener;
    }

    public @Nullable Integer component1() {
        return myPriority;
    }

    public String component2() {
        return myListener;
    }

    public SymfonyProfilerEventListener copy(@Nullable Integer priority, String listener) {
        return new SymfonyProfilerEventListener(priority, listener);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerEventListener that)) {
            return false;
        }
        return Objects.equals(myPriority, that.myPriority)
            && myListener.equals(that.myListener);
    }

    @Override
    public int hashCode() {
        int result = (myPriority == null ? 0 : myPriority.hashCode());
        result = 31 * result + myListener.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerEventListener(priority=" + myPriority + ", listener=" + myListener + ")";
    }
}
