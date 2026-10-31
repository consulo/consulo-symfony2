package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpCustomObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpEnum;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpNull;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObjectProperty;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpReference;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Expands Symfony VarDumper `Data` values after the lossless PHP unserializer has run.
 *
 * Descriptor arrays are interpreted only inside a confirmed VarDumper data table. Ordinary PHP
 * arrays therefore keep their original meaning when this reader is used for older collectors.
 */
public final class SymfonyVarDumperDataReader {
    private static final String VAR_DUMPER_NAMESPACE = "Symfony\\Component\\VarDumper\\";
    private static final String DATA_CLASS = VAR_DUMPER_NAMESPACE + "Cloner\\Data";
    private static final String STUB_CLASS = VAR_DUMPER_NAMESPACE + "Cloner\\Stub";
    private static final int TYPE_REFERENCE = 1;
    private static final int TYPE_STRING = 2;
    private static final int TYPE_ARRAY = 3;

    private final PhpUnserializeResult myResult;

    public SymfonyVarDumperDataReader(PhpUnserializeResult result) {
        myResult = result;
    }

    public ProfilerValue read(@Nullable PhpValue value) {
        PhpValue resolved = resolve(value);
        if (resolved instanceof PhpObject object) {
            return isVarDumperData(object) ? readData(object) : convertObject(object, new LinkedHashSet<>());
        }
        return convertPlain(resolved, new LinkedHashSet<>());
    }

    private ProfilerValue readData(PhpObject dataObject) {
        if (!(resolve(dataObject.get("data")) instanceof PhpArray levels)) {
            throw new IllegalStateException("Symfony VarDumper Data does not contain its data table");
        }
        long position = resolve(dataObject.get("position")) instanceof PhpInteger integer ? integer.getValue() : 0L;
        PhpValue rawKey = resolve(dataObject.get("key"));
        PhpArrayKey key;
        if (rawKey == null) {
            key = new PhpIntegerKey(0);
        }
        else if (rawKey instanceof PhpInteger integerKey) {
            key = new PhpIntegerKey(integerKey.getValue());
        }
        else if (rawKey instanceof PhpString stringKey) {
            key = new PhpStringKey(stringKey.getBytes());
        }
        else {
            throw new IllegalStateException("Symfony VarDumper Data has an unsupported cursor key");
        }
        PhpValue root = value(level(levels, position), key);
        if (root == null) {
            throw new IllegalStateException("Symfony VarDumper Data cursor does not identify a value");
        }

        return expandDataItem(root, levels, new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    private ProfilerValue expandDataItem(
        PhpValue value,
        PhpArray levels,
        Set<Long> activePositions,
        Set<Integer> activeReferences
    ) {
        if (value instanceof PhpReference reference) {
            if (!activeReferences.add(reference.getId())) {
                return new ProfilerReference(reference.getId());
            }
            try {
                return expandDataItem(myResult.resolve(reference), levels, activePositions, activeReferences);
            }
            finally {
                activeReferences.remove(reference.getId());
            }
        }

        DataStub stub = dataStub(value);
        if (stub == null) {
            return convertPlain(value, activeReferences);
        }

        // Reference stubs without their own position wrap another scalar or stub value.
        if (stub.type() == TYPE_REFERENCE && stub.position() == 0L) {
            PhpValue stubValue = stub.value();
            return stubValue != null ? expandDataItem(stubValue, levels, activePositions, activeReferences) : ProfilerNull.INSTANCE;
        }
        if (stub.type() == TYPE_STRING) {
            return convertPlain(stub.value(), activeReferences);
        }
        if (stub.position() == 0L) {
            return new ProfilerArray(Collections.emptyList());
        }
        if (!activePositions.add(stub.position())) {
            return new ProfilerReference(stub.position());
        }

        try {
            PhpArray children = level(levels, stub.position());
            List<ProfilerEntry> entries = new ArrayList<>(children.getEntries().size());
            for (PhpArrayEntry entry : children.getEntries()) {
                entries.add(new ProfilerEntry(
                    compactKey(entry.getKey()),
                    expandDataItem(entry.getValue(), levels, activePositions, activeReferences)
                ));
            }
            return new ProfilerArray(entries);
        }
        finally {
            activePositions.remove(stub.position());
        }
    }

    private @Nullable DataStub dataStub(PhpValue value) {
        if (value instanceof PhpArray array) {
            if (array.getEntries().isEmpty()) {
                return null;
            }

            // VarDumper encodes non-empty arrays as [array kind => child position].
            PhpValue lastValue = array.getEntries().get(array.getEntries().size() - 1).getValue();
            if (!(resolve(lastValue) instanceof PhpInteger position)) {
                throw new IllegalStateException("Malformed Symfony VarDumper array descriptor");
            }
            return new DataStub(TYPE_ARRAY, position.getValue(), null);
        }
        if (value instanceof PhpObject object) {
            if (!isVarDumperStub(object)) {
                throw new IllegalStateException("Unsupported object in Symfony VarDumper data table");
            }
            return new DataStub(
                resolve(object.get("type")) instanceof PhpInteger type ? (int) type.getValue() : TYPE_REFERENCE,
                resolve(object.get("position")) instanceof PhpInteger position ? position.getValue() : 0L,
                object.get("value")
            );
        }
        return null;
    }

    private ProfilerValue convertPlain(@Nullable PhpValue value, Set<Integer> activeReferences) {
        if (value == null || value instanceof PhpNull) {
            return ProfilerNull.INSTANCE;
        }
        if (value instanceof PhpReference reference) {
            if (!activeReferences.add(reference.getId())) {
                return new ProfilerReference(reference.getId());
            }
            try {
                return convertPlain(myResult.resolve(reference), activeReferences);
            }
            finally {
                activeReferences.remove(reference.getId());
            }
        }
        if (value instanceof PhpBoolean booleanValue) {
            return new ProfilerBoolean(booleanValue.getValue());
        }
        if (value instanceof PhpInteger integer) {
            return new ProfilerInteger(integer.getValue());
        }
        if (value instanceof PhpFloat floatValue) {
            return new ProfilerFloat(floatValue.getValue());
        }
        if (value instanceof PhpString string) {
            return new ProfilerString(string.getBytes().compact());
        }
        if (value instanceof PhpArray array) {
            List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
            for (PhpArrayEntry entry : array.getEntries()) {
                entries.add(new ProfilerEntry(compactKey(entry.getKey()), convertPlain(entry.getValue(), activeReferences)));
            }
            return new ProfilerArray(entries);
        }
        if (value instanceof PhpObject object) {
            return isVarDumperData(object) ? readData(object) : convertObject(object, activeReferences);
        }
        if (value instanceof PhpEnum enumValue) {
            return new ProfilerEnum(enumValue.getEnumName().compact(), enumValue.getCaseName().compact());
        }

        // PhpValue is sealed: the remaining kind is PhpCustomObject
        PhpCustomObject customObject = (PhpCustomObject) value;
        return new ProfilerOpaque("custom PHP object", customObject.getPayload().getSize());
    }

    private ProfilerArray convertObject(PhpObject value, Set<Integer> activeReferences) {
        List<ProfilerEntry> entries = new ArrayList<>(value.getProperties().size());
        for (PhpObjectProperty property : value.getProperties()) {
            entries.add(new ProfilerEntry(compactKey(property.getKey()), convertPlain(property.getValue(), activeReferences)));
        }
        return new ProfilerArray(entries);
    }

    private PhpArray level(PhpArray levels, long position) {
        if (resolve(levels.get(position)) instanceof PhpArray array) {
            return array;
        }
        throw new IllegalStateException("Symfony VarDumper child position " + position + " is missing");
    }

    private static @Nullable PhpValue value(PhpArray array, PhpArrayKey key) {
        if (key instanceof PhpIntegerKey integerKey) {
            return array.get(integerKey.getValue());
        }
        return array.get(((PhpStringKey) key).getBytes());
    }

    private @Nullable PhpValue resolve(@Nullable PhpValue value) {
        return value instanceof PhpReference reference ? myResult.resolve(reference) : value;
    }

    private static boolean isVarDumperData(PhpObject object) {
        return DATA_CLASS.equals(object.getClassName().utf8StringOrNull());
    }

    private static boolean isVarDumperStub(PhpObject object) {
        String name = object.getClassName().utf8StringOrNull();
        if (name == null) {
            return false;
        }
        return name.equals(STUB_CLASS) || name.startsWith(VAR_DUMPER_NAMESPACE) && name.endsWith("Stub");
    }

    private static PhpArrayKey compactKey(PhpArrayKey key) {
        if (key instanceof PhpIntegerKey) {
            return key;
        }
        return new PhpStringKey(logicalPropertyName(((PhpStringKey) key).getBytes()).compact());
    }

    /** Removes PHP's NUL-delimited private/protected property scope from VarDumper keys. */
    private static PhpBytes logicalPropertyName(PhpBytes bytes) {
        if (bytes.getSize() < 3 || bytes.byteAt(0) != (byte) 0) {
            return bytes;
        }

        int separator = bytes.indexOf((byte) 0, 1);
        return separator > 1 ? bytes.slice(separator + 1, bytes.getSize() - separator - 1) : bytes;
    }

    private record DataStub(int type, long position, @Nullable PhpValue value) {
    }
}
