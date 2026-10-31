package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A symbolic one-based reference into {@link PhpUnserializeResult}'s PHP traversal table.
 */
public final class PhpReference implements PhpValue {
    private final int myId;
    private final PhpReferenceKind myKind;

    public PhpReference(int id, PhpReferenceKind kind) {
        myId = id;
        myKind = kind;
    }

    public int getId() {
        return myId;
    }

    public PhpReferenceKind getKind() {
        return myKind;
    }

    public int component1() {
        return myId;
    }

    public PhpReferenceKind component2() {
        return myKind;
    }

    public PhpReference copy(int id, PhpReferenceKind kind) {
        return new PhpReference(id, kind);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpReference that)) {
            return false;
        }
        return myId == that.myId && Objects.equals(myKind, that.myKind);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(myId);
        result = result * 31 + myKind.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpReference(id=" + myId + ", kind=" + myKind + ")";
    }
}
