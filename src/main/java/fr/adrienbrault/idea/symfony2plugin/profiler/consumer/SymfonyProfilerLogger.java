package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerLogger {
    /** Newest messages first. */
    private final List<SymfonyProfilerLog> myLogs;

    public SymfonyProfilerLogger(List<SymfonyProfilerLog> logs) {
        myLogs = logs;
    }

    /** Newest messages first. */
    public List<SymfonyProfilerLog> getLogs() {
        return myLogs;
    }

    public List<SymfonyProfilerLog> component1() {
        return myLogs;
    }

    public SymfonyProfilerLogger copy(List<SymfonyProfilerLog> logs) {
        return new SymfonyProfilerLogger(logs);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerLogger that)) {
            return false;
        }
        return myLogs.equals(that.myLogs);
    }

    @Override
    public int hashCode() {
        return myLogs.hashCode();
    }

    @Override
    public String toString() {
        return "SymfonyProfilerLogger(logs=" + myLogs + ")";
    }
}
