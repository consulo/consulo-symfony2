package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One logical Twig extension symbol like `form_start` in `{{ form_start() }}`.
 */
public final class TwigExtensionUsageSymbol {
    /**
     * Symbol kind such as function or filter.
     */
    private final TwigExtensionUsageKind myKind;

    /**
     * Canonical registered Twig name like `form_start` or `trans`.
     */
    private final String myName;

    public TwigExtensionUsageSymbol(TwigExtensionUsageKind kind, String name) {
        myKind = kind;
        myName = name;
    }

    public TwigExtensionUsageKind getKind() {
        return myKind;
    }

    public String getName() {
        return myName;
    }

    public TwigExtensionUsageKind component1() {
        return myKind;
    }

    public String component2() {
        return myName;
    }

    public TwigExtensionUsageSymbol copy(TwigExtensionUsageKind kind, String name) {
        return new TwigExtensionUsageSymbol(kind, name);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigExtensionUsageSymbol)) {
            return false;
        }
        TwigExtensionUsageSymbol that = (TwigExtensionUsageSymbol) o;
        return myKind == that.myKind && Objects.equals(myName, that.myName);
    }

    @Override
    public int hashCode() {
        return myKind.hashCode() * 31 + myName.hashCode();
    }

    @Override
    public String toString() {
        return "TwigExtensionUsageSymbol(kind=" + myKind + ", name=" + myName + ")";
    }
}
