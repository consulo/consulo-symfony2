package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A root value plus the reference table needed to navigate aliases and cycles safely.
 */
public final class PhpUnserializeResult {
    private final PhpValue myRoot;
    private final List<PhpValue> myReferences;
    private final int myParsedValueCount;

    public PhpUnserializeResult(PhpValue root, List<PhpValue> references, int parsedValueCount) {
        myRoot = root;
        myReferences = references;
        myParsedValueCount = parsedValueCount;
    }

    public PhpValue getRoot() {
        return myRoot;
    }

    public int getParsedValueCount() {
        return myParsedValueCount;
    }

    public int getReferenceCount() {
        return myReferences.size();
    }

    /**
     * Resolves a symbolic reference without recursively expanding the graph.
     * <p>
     * Example: {@code result.resolve(PhpValueKt.requireReference(array.get("self")))}
     */
    public PhpValue resolve(PhpReference reference) {
        PhpReference current = reference;
        Set<Integer> visited = new HashSet<>();

        while (true) {
            if (current.getId() < 1 || current.getId() > myReferences.size()) {
                throw new IllegalArgumentException("Reference ID is outside this result");
            }
            if (!visited.add(current.getId())) {
                throw new IllegalArgumentException("Reference cycle in reference table");
            }

            PhpValue value = myReferences.get(current.getId() - 1);
            if (!(value instanceof PhpReference phpReference)) {
                return value;
            }
            current = phpReference;
        }
    }

    @Override
    public String toString() {
        return "PhpUnserializeResult(root=" + myRoot.getClass().getSimpleName() + ", referenceCount=" + getReferenceCount() + ", parsedValueCount=" + myParsedValueCount + ")";
    }
}
