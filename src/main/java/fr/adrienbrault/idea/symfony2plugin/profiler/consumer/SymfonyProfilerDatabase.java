package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerDatabase {
    private final int myQueryCount;
    private final double myTotalTimeMs;
    private final List<String> myConnections;
    private final int myDuplicateQueryCount;
    private final List<SymfonyProfilerDatabaseQueryGroup> myQueryGroups;

    public SymfonyProfilerDatabase(int queryCount, double totalTimeMs, List<String> connections, int duplicateQueryCount, List<SymfonyProfilerDatabaseQueryGroup> queryGroups) {
        myQueryCount = queryCount;
        myTotalTimeMs = totalTimeMs;
        myConnections = connections;
        myDuplicateQueryCount = duplicateQueryCount;
        myQueryGroups = queryGroups;
    }

    public int getQueryCount() {
        return myQueryCount;
    }

    public double getTotalTimeMs() {
        return myTotalTimeMs;
    }

    public List<String> getConnections() {
        return myConnections;
    }

    public int getDuplicateQueryCount() {
        return myDuplicateQueryCount;
    }

    public List<SymfonyProfilerDatabaseQueryGroup> getQueryGroups() {
        return myQueryGroups;
    }

    public int component1() {
        return myQueryCount;
    }

    public double component2() {
        return myTotalTimeMs;
    }

    public List<String> component3() {
        return myConnections;
    }

    public int component4() {
        return myDuplicateQueryCount;
    }

    public List<SymfonyProfilerDatabaseQueryGroup> component5() {
        return myQueryGroups;
    }

    public SymfonyProfilerDatabase copy(int queryCount, double totalTimeMs, List<String> connections, int duplicateQueryCount, List<SymfonyProfilerDatabaseQueryGroup> queryGroups) {
        return new SymfonyProfilerDatabase(queryCount, totalTimeMs, connections, duplicateQueryCount, queryGroups);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerDatabase that)) {
            return false;
        }
        return myQueryCount == that.myQueryCount
            && Double.compare(myTotalTimeMs, that.myTotalTimeMs) == 0
            && myConnections.equals(that.myConnections)
            && myDuplicateQueryCount == that.myDuplicateQueryCount
            && myQueryGroups.equals(that.myQueryGroups);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(myQueryCount);
        result = 31 * result + Double.hashCode(myTotalTimeMs);
        result = 31 * result + myConnections.hashCode();
        result = 31 * result + Integer.hashCode(myDuplicateQueryCount);
        result = 31 * result + myQueryGroups.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerDatabase(queryCount=" + myQueryCount + ", totalTimeMs=" + myTotalTimeMs + ", connections=" + myConnections + ", duplicateQueryCount=" + myDuplicateQueryCount + ", queryGroups=" + myQueryGroups + ")";
    }
}
