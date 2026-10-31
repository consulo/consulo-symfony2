package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Passively parses one PHP {@code serialize()} byte stream without loading PHP classes or executing code.
 * <p>
 * Example:
 * <pre>{@code
 * PhpUnserializeResult result = PhpUnserializer.unserialize("a:1:{s:4:\"name\";s:3:\"Ada\";}".getBytes(StandardCharsets.UTF_8), new PhpUnserializeLimits());
 * String name = PhpValueKt.utf8StringOrNull(PhpValueKt.requireArray(result.getRoot()).get("name"));
 * }</pre>
 */
public final class PhpUnserializer {
    public static final PhpUnserializer INSTANCE = new PhpUnserializer();

    private PhpUnserializer() {
    }

    /**
     * Parses exactly one value and rejects trailing bytes. The input is defensively copied once,
     * and {@code limits} apply only to this call.
     */
    public static PhpUnserializeResult unserialize(byte[] input, PhpUnserializeLimits limits) {
        if (input.length > limits.getMaxInputBytes()) {
            throw new PhpUnserializeException(
                limits.getMaxInputBytes(),
                null,
                "input size " + input.length + " exceeds limit " + limits.getMaxInputBytes()
            );
        }

        return new Parser(Arrays.copyOf(input, input.length), limits).parse();
    }

    private static final class Parser {
        private static final int MAX_INITIAL_CONTAINER_CAPACITY = 1024;

        private final byte[] myInput;
        private final PhpUnserializeLimits myLimits;
        private int myOffset = 0;
        private int myParsedValueCount = 0;
        private final List<ReferenceSlot> myReferenceSlots = new ArrayList<>();

        private Parser(byte[] input, PhpUnserializeLimits limits) {
            myInput = input;
            myLimits = limits;
        }

        PhpUnserializeResult parse() {
            PhpValue root = parseValue(0);
            if (myOffset != myInput.length) {
                throw fail(myOffset, "trailing bytes after root value", asChar(myInput[myOffset]));
            }

            List<PhpValue> references = new ArrayList<>(myReferenceSlots.size());
            for (int index = 0; index < myReferenceSlots.size(); index++) {
                PhpValue value = myReferenceSlots.get(index).myValue;
                if (value == null) {
                    throw fail(myInput.length, "unresolved reference slot " + (index + 1));
                }
                references.add(value);
            }
            return new PhpUnserializeResult(root, references, myParsedValueCount);
        }

        private PhpValue parseValue(int containerDepth) {
            int start = myOffset;
            char tag = readTag();
            myParsedValueCount++;
            if (myParsedValueCount > myLimits.getMaxTotalValues()) {
                throw fail(start, "parsed value limit " + myLimits.getMaxTotalValues() + " exceeded", tag);
            }

            return switch (tag) {
                case 'N' -> parseNull(tag);
                case 'b' -> parseBoolean(tag);
                case 'i' -> parseInteger(tag);
                case 'd' -> parseFloat(tag);
                case 's' -> register(parseString(tag), ReferenceType.VALUE);
                case 'a' -> parseArray(containerDepth, start, tag);
                case 'O' -> parseObject(containerDepth, start, tag);
                case 'E' -> parseEnum(start, tag);
                case 'r' -> parseReference(tag, PhpReferenceKind.OBJECT);
                case 'R' -> parseReference(tag, PhpReferenceKind.ALIAS);
                case 'C' -> parseCustomObject(tag);
                default -> throw fail(start, "unsupported serialization tag", tag);
            };
        }

        private PhpValue parseNull(char tag) {
            expect(';', tag);
            return register(PhpNull.INSTANCE, ReferenceType.VALUE);
        }

        private PhpValue parseBoolean(char tag) {
            expect(':', tag);
            int valueOffset = myOffset;
            byte b = readByte(tag);
            boolean value;
            if (b == (byte) '0') {
                value = false;
            }
            else if (b == (byte) '1') {
                value = true;
            }
            else {
                throw fail(valueOffset, "boolean must be 0 or 1", tag);
            }
            expect(';', tag);
            return register(new PhpBoolean(value), ReferenceType.VALUE);
        }

        private PhpValue parseInteger(char tag) {
            expect(':', tag);
            long value = readSignedLong(';', "integer", tag);
            return register(new PhpInteger(value), ReferenceType.VALUE);
        }

        private PhpValue parseFloat(char tag) {
            expect(':', tag);
            int valueOffset = myOffset;
            int end = findDelimiter(';', tag);
            int length = end - valueOffset;
            if (length == 0) {
                throw fail(valueOffset, "double is empty", tag);
            }

            double value;
            if (matchesAscii(valueOffset, length, "INF")) {
                value = Double.POSITIVE_INFINITY;
            }
            else if (matchesAscii(valueOffset, length, "-INF")) {
                value = Double.NEGATIVE_INFINITY;
            }
            else if (matchesAscii(valueOffset, length, "NAN")) {
                value = Double.NaN;
            }
            else {
                value = parseFiniteDouble(valueOffset, length, tag);
            }
            myOffset = end + 1;
            return register(new PhpFloat(value), ReferenceType.VALUE);
        }

        private PhpString parseString(char tag) {
            expect(':', tag);
            PhpBytes bytes = readQuotedBytes("string length", tag);
            expect(';', tag);
            return new PhpString(bytes);
        }

        private PhpArray parseArray(int containerDepth, int start, char tag) {
            ensureContainerDepth(containerDepth, start, tag);
            expect(':', tag);
            int countOffset = myOffset;
            int count = readUnsignedInt(':', "array entry count", tag);
            ensureContainerCount(count, countOffset, tag);
            expect('{', tag);

            ReferenceSlot slot = reserveSlot(ReferenceType.ARRAY);
            List<PhpArrayEntry> entries = new ArrayList<>(Math.min(count, MAX_INITIAL_CONTAINER_CAPACITY));
            for (int i = 0; i < count; i++) {
                PhpArrayKey key = parseArrayKey("array key");
                PhpValue value = parseValue(containerDepth + 1);
                entries.add(new PhpArrayEntry(key, value));
            }
            expect('}', tag);

            PhpArray value = new PhpArray(Collections.unmodifiableList(entries));
            slot.myValue = value;
            return value;
        }

        private PhpArrayKey parseArrayKey(String context) {
            int start = myOffset;
            char tag = readTag();
            switch (tag) {
                case 'i': {
                    expect(':', tag);
                    return new PhpIntegerKey(readSignedLong(';', "array integer key", tag));
                }
                case 's': {
                    expect(':', tag);
                    PhpBytes bytes = readQuotedBytes("array key length", tag);
                    expect(';', tag);
                    return new PhpStringKey(bytes);
                }
                default:
                    throw fail(start, context + " must use tag 'i' or 's'", tag);
            }
        }

        private PhpObject parseObject(int containerDepth, int start, char tag) {
            ensureContainerDepth(containerDepth, start, tag);
            expect(':', tag);
            PhpBytes className = readQuotedBytes("object class-name length", tag);
            expect(':', tag);
            int countOffset = myOffset;
            int count = readUnsignedInt(':', "object property count", tag);
            ensureContainerCount(count, countOffset, tag);
            expect('{', tag);

            ReferenceSlot slot = reserveSlot(ReferenceType.OBJECT);
            List<PhpObjectProperty> properties = new ArrayList<>(Math.min(count, MAX_INITIAL_CONTAINER_CAPACITY));
            for (int i = 0; i < count; i++) {
                PhpArrayKey key = parseArrayKey("object property key");
                properties.add(PhpValueKt.createPhpObjectProperty(key, parseValue(containerDepth + 1)));
            }
            expect('}', tag);

            PhpObject value = new PhpObject(className, Collections.unmodifiableList(properties));
            slot.myValue = value;
            return value;
        }

        private PhpEnum parseEnum(int start, char tag) {
            expect(':', tag);
            PhpBytes encoded = readQuotedBytes("enum name length", tag);
            expect(';', tag);
            int separator = encoded.indexOf((byte) ':', 0);
            if (separator <= 0 || separator == encoded.getSize() - 1) {
                throw fail(start, "enum value must contain a non-empty enum and case name", tag);
            }

            PhpEnum value = new PhpEnum(
                encoded.slice(0, separator),
                encoded.slice(separator + 1, encoded.getSize() - separator - 1)
            );
            return register(value, ReferenceType.OBJECT);
        }

        private PhpCustomObject parseCustomObject(char tag) {
            expect(':', tag);
            PhpBytes className = readQuotedBytes("custom-object class-name length", tag);
            expect(':', tag);
            int payloadLengthOffset = myOffset;
            int payloadLength = readUnsignedInt(':', "custom-object payload length", tag);
            ensurePayloadLength(payloadLength, payloadLengthOffset, tag);
            expect('{', tag);
            PhpBytes payload = readPayload(payloadLength, "custom-object payload", tag);
            expect('}', tag);
            return register(new PhpCustomObject(className, payload), ReferenceType.OBJECT);
        }

        // PHP's r token gets a new traversal slot, while R aliases the existing slot directly.
        private PhpReference parseReference(char tag, PhpReferenceKind kind) {
            expect(':', tag);
            int idOffset = myOffset;
            int id = readUnsignedInt(';', "reference ID", tag);
            if (id == 0 || id > myReferenceSlots.size()) {
                throw fail(idOffset, "reference ID " + id + " does not identify an earlier value", tag);
            }

            ReferenceSlot target = myReferenceSlots.get(id - 1);
            if (kind == PhpReferenceKind.OBJECT && target.myType != ReferenceType.OBJECT) {
                throw fail(idOffset, "object reference does not identify an object value", tag);
            }

            PhpReference reference = new PhpReference(id, kind);
            if (kind == PhpReferenceKind.OBJECT) {
                register(reference, ReferenceType.OBJECT);
            }
            return reference;
        }

        private PhpBytes readQuotedBytes(String field, char tag) {
            int lengthOffset = myOffset;
            int length = readUnsignedInt(':', field, tag);
            ensurePayloadLength(length, lengthOffset, tag);
            expect('"', tag);
            PhpBytes bytes = readPayload(length, field, tag);
            expect('"', tag);
            return bytes;
        }

        private PhpBytes readPayload(int length, String field, char tag) {
            if (length > myInput.length - myOffset) {
                throw fail(myOffset, field + " is truncated", tag);
            }
            PhpBytes bytes = PhpBytes.Companion.view(myInput, myOffset, length);
            myOffset += length;
            return bytes;
        }

        private int readUnsignedInt(char delimiter, String field, char tag) {
            int start = myOffset;
            if (myOffset >= myInput.length) {
                throw fail(myOffset, field + " is missing", tag);
            }
            if (myInput[myOffset] == (byte) '-') {
                throw fail(start, field + " must not be negative", tag);
            }

            int result = 0;
            int digits = 0;
            while (myOffset < myInput.length && myInput[myOffset] != (byte) delimiter) {
                int digit = myInput[myOffset] - '0';
                if (digit < 0 || digit > 9) {
                    throw fail(myOffset, field + " contains a non-digit", tag);
                }
                if (result > (Integer.MAX_VALUE - digit) / 10) {
                    throw fail(start, field + " overflows an integer", tag);
                }
                result = result * 10 + digit;
                digits++;
                myOffset++;
            }
            if (digits == 0) {
                throw fail(start, field + " is empty", tag);
            }
            expect(delimiter, tag);
            return result;
        }

        private long readSignedLong(char delimiter, String field, char tag) {
            int start = myOffset;
            if (myOffset >= myInput.length) {
                throw fail(myOffset, field + " is missing", tag);
            }

            boolean negative = false;
            if (myInput[myOffset] == (byte) '-' || myInput[myOffset] == (byte) '+') {
                negative = myInput[myOffset] == (byte) '-';
                myOffset++;
            }
            int digitStart = myOffset;
            long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
            long multiplyLimit = limit / 10;
            long result = 0L;

            while (myOffset < myInput.length && myInput[myOffset] != (byte) delimiter) {
                int digit = myInput[myOffset] - '0';
                if (digit < 0 || digit > 9) {
                    throw fail(myOffset, field + " contains a non-digit", tag);
                }
                if (result < multiplyLimit) {
                    throw fail(start, field + " overflows a long", tag);
                }
                result *= 10;
                if (result < limit + digit) {
                    throw fail(start, field + " overflows a long", tag);
                }
                result -= digit;
                myOffset++;
            }
            if (myOffset == digitStart) {
                throw fail(start, field + " is empty", tag);
            }
            expect(delimiter, tag);
            return negative ? result : -result;
        }

        private double parseFiniteDouble(int start, int length, char tag) {
            int index = start;
            int end = start + length;
            if (index < end && (myInput[index] == (byte) '+' || myInput[index] == (byte) '-')) {
                index++;
            }

            int digits = 0;
            while (index < end && isAsciiDigit(myInput[index])) {
                digits++;
                index++;
            }
            if (index < end && myInput[index] == (byte) '.') {
                index++;
                while (index < end && isAsciiDigit(myInput[index])) {
                    digits++;
                    index++;
                }
            }
            if (digits == 0) {
                throw fail(start, "double has invalid syntax", tag);
            }
            if (index < end && (myInput[index] == (byte) 'e' || myInput[index] == (byte) 'E')) {
                index++;
                if (index < end && (myInput[index] == (byte) '+' || myInput[index] == (byte) '-')) {
                    index++;
                }
                int exponentStart = index;
                while (index < end && isAsciiDigit(myInput[index])) {
                    index++;
                }
                if (index == exponentStart) {
                    throw fail(start, "double exponent is empty", tag);
                }
            }
            if (index != end) {
                throw fail(index, "double has invalid syntax", tag);
            }

            String text = new String(myInput, start, length, StandardCharsets.US_ASCII);
            double value;
            try {
                value = Double.parseDouble(text);
            }
            catch (NumberFormatException e) {
                throw fail(start, "double cannot be parsed", tag);
            }
            if (!Double.isFinite(value)) {
                throw fail(start, "finite double overflows", tag);
            }
            return value;
        }

        private static boolean isAsciiDigit(byte b) {
            return b >= (byte) '0' && b <= (byte) '9';
        }

        private int findDelimiter(char delimiter, char tag) {
            for (int index = myOffset; index < myInput.length; index++) {
                if (myInput[index] == (byte) delimiter) {
                    return index;
                }
            }
            throw fail(myInput.length, "expected '" + delimiter + "' before end of input", tag);
        }

        private boolean matchesAscii(int start, int length, String expected) {
            if (length != expected.length()) {
                return false;
            }
            for (int index = 0; index < expected.length(); index++) {
                if (myInput[start + index] != (byte) expected.charAt(index)) {
                    return false;
                }
            }
            return true;
        }

        private void ensureContainerDepth(int containerDepth, int start, char tag) {
            if (containerDepth >= myLimits.getMaxNestingDepth()) {
                throw fail(start, "nesting depth limit " + myLimits.getMaxNestingDepth() + " exceeded", tag);
            }
        }

        private void ensureContainerCount(int count, int countOffset, char tag) {
            if (count > myLimits.getMaxContainerEntries()) {
                throw fail(countOffset, "container entry count " + count + " exceeds limit " + myLimits.getMaxContainerEntries(), tag);
            }
        }

        private void ensurePayloadLength(int length, int lengthOffset, char tag) {
            if (length > myLimits.getMaxPayloadBytes()) {
                throw fail(lengthOffset, "payload length " + length + " exceeds limit " + myLimits.getMaxPayloadBytes(), tag);
            }
        }

        private <T extends PhpValue> T register(T value, ReferenceType type) {
            myReferenceSlots.add(new ReferenceSlot(type, value));
            return value;
        }

        private ReferenceSlot reserveSlot(ReferenceType type) {
            ReferenceSlot slot = new ReferenceSlot(type, null);
            myReferenceSlots.add(slot);
            return slot;
        }

        private char readTag() {
            if (myOffset >= myInput.length) {
                throw fail(myOffset, "expected a value");
            }
            return asChar(myInput[myOffset++]);
        }

        private byte readByte(char tag) {
            if (myOffset >= myInput.length) {
                throw fail(myOffset, "unexpected end of input", tag);
            }
            return myInput[myOffset++];
        }

        private void expect(char expected, char tag) {
            if (myOffset >= myInput.length) {
                throw fail(myOffset, "expected '" + expected + "' before end of input", tag);
            }
            if (myInput[myOffset] != (byte) expected) {
                throw fail(myOffset, "expected '" + expected + "'", tag);
            }
            myOffset++;
        }

        // Kotlin: fun fail(...): Nothing; returned here so call sites can use "throw fail(...)"
        private static PhpUnserializeException fail(int failureOffset, String reason) {
            return fail(failureOffset, reason, null);
        }

        private static PhpUnserializeException fail(int failureOffset, String reason, @Nullable Character tag) {
            return new PhpUnserializeException(failureOffset, tag, reason);
        }

        private static char asChar(byte b) {
            return (char) (b & 0xff);
        }

        private static final class ReferenceSlot {
            private final ReferenceType myType;
            private @Nullable PhpValue myValue;

            private ReferenceSlot(ReferenceType type, @Nullable PhpValue value) {
                myType = type;
                myValue = value;
            }
        }

        private enum ReferenceType {
            VALUE,
            ARRAY,
            OBJECT,
        }
    }
}
