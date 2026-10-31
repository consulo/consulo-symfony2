package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * An immutable byte string.
 * <p>
 * Parsed instances are views into the unserializer's private input snapshot. Retaining one of
 * those instances therefore retains that snapshot. Public byte-array access always returns a
 * copy, so neither the caller's original input nor a returned array can mutate the value graph.
 */
public final class PhpBytes {
    public static final Companion Companion = new Companion();

    private final byte[] myStorage;
    private final int myOffset;
    private final int mySize;

    private PhpBytes(byte[] storage, int offset, int size) {
        myStorage = storage;
        myOffset = offset;
        mySize = size;
    }

    public PhpBytes(byte[] bytes) {
        this(Arrays.copyOf(bytes, bytes.length), 0, bytes.length);
    }

    public int getSize() {
        return mySize;
    }

    /**
     * Decodes with strict UTF-8 rules and reports malformed or unmappable bytes.
     */
    public String utf8String() throws CharacterCodingException {
        return StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(myStorage, myOffset, mySize))
            .toString();
    }

    /**
     * Returns strict UTF-8 text, or {@code null} when this is a binary string.
     */
    public @Nullable String utf8StringOrNull() {
        try {
            return utf8String();
        }
        catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * Returns a mutable copy; changing it cannot affect the parsed graph.
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(myStorage, myOffset, myOffset + mySize);
    }

    /**
     * Returns an equal byte string backed only by its own bytes.
     * <p>
     * Use this when retaining a small parsed value after discarding the larger parse result;
     * parsed views otherwise keep the unserializer's complete input snapshot alive.
     */
    public PhpBytes compact() {
        if (myOffset == 0 && mySize == myStorage.length) {
            return this;
        }

        return new PhpBytes(Arrays.copyOfRange(myStorage, myOffset, myOffset + mySize), 0, mySize);
    }

    public boolean contentEquals(byte[] other) {
        if (mySize != other.length) {
            return false;
        }

        for (int index = 0; index < mySize; index++) {
            if (myStorage[myOffset + index] != other[index]) {
                return false;
            }
        }

        return true;
    }

    public byte byteAt(int index) {
        if (index < 0 || index >= mySize) {
            throw new IllegalArgumentException("Byte index out of bounds");
        }
        return myStorage[myOffset + index];
    }

    public int indexOf(byte value, int fromIndex) {
        if (fromIndex < 0 || fromIndex > mySize) {
            throw new IllegalArgumentException("Byte index out of bounds");
        }
        for (int index = fromIndex; index < mySize; index++) {
            if (myStorage[myOffset + index] == value) {
                return index;
            }
        }

        return -1;
    }

    public PhpBytes slice(int sliceOffset, int sliceLength) {
        if (!(sliceOffset >= 0 && sliceLength >= 0 && sliceOffset <= mySize - sliceLength)) {
            throw new IllegalArgumentException("Byte slice out of bounds");
        }
        return new PhpBytes(myStorage, myOffset + sliceOffset, sliceLength);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PhpBytes that) || mySize != that.mySize) {
            return false;
        }

        for (int index = 0; index < mySize; index++) {
            if (myStorage[myOffset + index] != that.myStorage[that.myOffset + index]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int index = 0; index < mySize; index++) {
            result = 31 * result + myStorage[myOffset + index];
        }
        return result;
    }

    @Override
    public String toString() {
        return "PhpBytes(size=" + mySize + ")";
    }

    public static final class Companion {
        private Companion() {
        }

        public PhpBytes view(byte[] storage, int offset, int length) {
            return new PhpBytes(storage, offset, length);
        }

        public PhpBytes utf8(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            return new PhpBytes(bytes, 0, bytes.length);
        }
    }
}
