package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * One detail page whose entries are always kept intact.
 */
public final class ProfilerDetailPage {
    private final List<String> myEntries;
    private final int myNumber;
    private final int myTotal;

    public ProfilerDetailPage(List<String> entries, int number, int total) {
        myEntries = entries;
        myNumber = number;
        myTotal = total;
    }

    public List<String> getEntries() {
        return myEntries;
    }

    public int getNumber() {
        return myNumber;
    }

    public int getTotal() {
        return myTotal;
    }

    public boolean isPaginated() {
        return myTotal > 1;
    }

    public List<String> component1() {
        return myEntries;
    }

    public int component2() {
        return myNumber;
    }

    public int component3() {
        return myTotal;
    }

    public ProfilerDetailPage copy(List<String> entries, int number, int total) {
        return new ProfilerDetailPage(entries, number, total);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerDetailPage that)) {
            return false;
        }
        return myNumber == that.myNumber && myTotal == that.myTotal && Objects.equals(myEntries, that.myEntries);
    }

    @Override
    public int hashCode() {
        int result = myEntries.hashCode();
        result = 31 * result + Integer.hashCode(myNumber);
        result = 31 * result + Integer.hashCode(myTotal);
        return result;
    }

    @Override
    public String toString() {
        return "ProfilerDetailPage(entries=" + myEntries + ", number=" + myNumber + ", total=" + myTotal + ")";
    }
}
