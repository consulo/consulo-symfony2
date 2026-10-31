package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerDatabaseQueryGroup {
    private final String mySql;
    private final int myCount;
    private final double myTotalTimeMs;
    private final double myAverageTimeMs;
    /** One complete trace per query occurrence; frame arguments are never retained. */
    private final List<List<SymfonyProfilerDatabaseStackFrame>> myStackTraces;

    public SymfonyProfilerDatabaseQueryGroup(String sql, int count, double totalTimeMs, double averageTimeMs, List<List<SymfonyProfilerDatabaseStackFrame>> stackTraces) {
        mySql = sql;
        myCount = count;
        myTotalTimeMs = totalTimeMs;
        myAverageTimeMs = averageTimeMs;
        myStackTraces = stackTraces;
    }

    public String getSql() {
        return mySql;
    }

    public int getCount() {
        return myCount;
    }

    public double getTotalTimeMs() {
        return myTotalTimeMs;
    }

    public double getAverageTimeMs() {
        return myAverageTimeMs;
    }

    /** One complete trace per query occurrence; frame arguments are never retained. */
    public List<List<SymfonyProfilerDatabaseStackFrame>> getStackTraces() {
        return myStackTraces;
    }

    public String component1() {
        return mySql;
    }

    public int component2() {
        return myCount;
    }

    public double component3() {
        return myTotalTimeMs;
    }

    public double component4() {
        return myAverageTimeMs;
    }

    public List<List<SymfonyProfilerDatabaseStackFrame>> component5() {
        return myStackTraces;
    }

    public SymfonyProfilerDatabaseQueryGroup copy(String sql, int count, double totalTimeMs, double averageTimeMs, List<List<SymfonyProfilerDatabaseStackFrame>> stackTraces) {
        return new SymfonyProfilerDatabaseQueryGroup(sql, count, totalTimeMs, averageTimeMs, stackTraces);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerDatabaseQueryGroup that)) {
            return false;
        }
        return mySql.equals(that.mySql)
            && myCount == that.myCount
            && Double.compare(myTotalTimeMs, that.myTotalTimeMs) == 0
            && Double.compare(myAverageTimeMs, that.myAverageTimeMs) == 0
            && myStackTraces.equals(that.myStackTraces);
    }

    @Override
    public int hashCode() {
        int result = mySql.hashCode();
        result = 31 * result + Integer.hashCode(myCount);
        result = 31 * result + Double.hashCode(myTotalTimeMs);
        result = 31 * result + Double.hashCode(myAverageTimeMs);
        result = 31 * result + myStackTraces.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerDatabaseQueryGroup(sql=" + mySql + ", count=" + myCount + ", totalTimeMs=" + myTotalTimeMs + ", averageTimeMs=" + myAverageTimeMs + ", stackTraces=" + myStackTraces + ")";
    }
}
