package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

/**
 * Defensive bounds checked before input copying, allocation, or recursive descent.
 */
public final class PhpUnserializeLimits {
    private final int myMaxInputBytes;
    private final int myMaxNestingDepth;
    private final int myMaxTotalValues;
    private final int myMaxContainerEntries;
    private final int myMaxPayloadBytes;

    public PhpUnserializeLimits(int maxInputBytes, int maxNestingDepth, int maxTotalValues, int maxContainerEntries, int maxPayloadBytes) {
        myMaxInputBytes = maxInputBytes;
        myMaxNestingDepth = maxNestingDepth;
        myMaxTotalValues = maxTotalValues;
        myMaxContainerEntries = maxContainerEntries;
        myMaxPayloadBytes = maxPayloadBytes;

        if (maxInputBytes < 0) {
            throw new IllegalArgumentException("maxInputBytes must not be negative");
        }
        if (maxNestingDepth < 0) {
            throw new IllegalArgumentException("maxNestingDepth must not be negative");
        }
        if (maxTotalValues < 0) {
            throw new IllegalArgumentException("maxTotalValues must not be negative");
        }
        if (maxContainerEntries < 0) {
            throw new IllegalArgumentException("maxContainerEntries must not be negative");
        }
        if (maxPayloadBytes < 0) {
            throw new IllegalArgumentException("maxPayloadBytes must not be negative");
        }
    }

    /**
     * Kotlin generates a no-arg constructor because every parameter has a default value.
     */
    public PhpUnserializeLimits() {
        this(5 * 1024 * 1024, 256, 250_000, 100_000, 2 * 1024 * 1024);
    }

    public int getMaxInputBytes() {
        return myMaxInputBytes;
    }

    public int getMaxNestingDepth() {
        return myMaxNestingDepth;
    }

    public int getMaxTotalValues() {
        return myMaxTotalValues;
    }

    public int getMaxContainerEntries() {
        return myMaxContainerEntries;
    }

    public int getMaxPayloadBytes() {
        return myMaxPayloadBytes;
    }

    public int component1() {
        return myMaxInputBytes;
    }

    public int component2() {
        return myMaxNestingDepth;
    }

    public int component3() {
        return myMaxTotalValues;
    }

    public int component4() {
        return myMaxContainerEntries;
    }

    public int component5() {
        return myMaxPayloadBytes;
    }

    public PhpUnserializeLimits copy(int maxInputBytes, int maxNestingDepth, int maxTotalValues, int maxContainerEntries, int maxPayloadBytes) {
        return new PhpUnserializeLimits(maxInputBytes, maxNestingDepth, maxTotalValues, maxContainerEntries, maxPayloadBytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpUnserializeLimits that)) {
            return false;
        }
        return myMaxInputBytes == that.myMaxInputBytes
            && myMaxNestingDepth == that.myMaxNestingDepth
            && myMaxTotalValues == that.myMaxTotalValues
            && myMaxContainerEntries == that.myMaxContainerEntries
            && myMaxPayloadBytes == that.myMaxPayloadBytes;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(myMaxInputBytes);
        result = result * 31 + Integer.hashCode(myMaxNestingDepth);
        result = result * 31 + Integer.hashCode(myMaxTotalValues);
        result = result * 31 + Integer.hashCode(myMaxContainerEntries);
        result = result * 31 + Integer.hashCode(myMaxPayloadBytes);
        return result;
    }

    @Override
    public String toString() {
        return "PhpUnserializeLimits(maxInputBytes=" + myMaxInputBytes
            + ", maxNestingDepth=" + myMaxNestingDepth
            + ", maxTotalValues=" + myMaxTotalValues
            + ", maxContainerEntries=" + myMaxContainerEntries
            + ", maxPayloadBytes=" + myMaxPayloadBytes + ")";
    }
}
