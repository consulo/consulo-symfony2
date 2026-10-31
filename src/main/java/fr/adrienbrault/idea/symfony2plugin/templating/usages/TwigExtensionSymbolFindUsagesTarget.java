package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Twig extension usage target that keeps the Twig symbol itself as the usage identity, for example `form_start` or `trans`.
 */
public final class TwigExtensionSymbolFindUsagesTarget implements TwigFindUsagesTarget {
    private final PsiElement myPrimaryElement;
    private final TwigExtensionUsageSymbol mySymbol;
    private final List<PsiElement> myPrimaryElements;

    public TwigExtensionSymbolFindUsagesTarget(PsiElement primaryElement, TwigExtensionUsageSymbol symbol) {
        myPrimaryElement = primaryElement;
        mySymbol = symbol;
        myPrimaryElements = Collections.singletonList(primaryElement);
    }

    @Override
    public PsiElement getPrimaryElement() {
        return myPrimaryElement;
    }

    public TwigExtensionUsageSymbol getSymbol() {
        return mySymbol;
    }

    @Override
    public List<PsiElement> getPrimaryElements() {
        return myPrimaryElements;
    }

    public PsiElement component1() {
        return myPrimaryElement;
    }

    public TwigExtensionUsageSymbol component2() {
        return mySymbol;
    }

    public TwigExtensionSymbolFindUsagesTarget copy(PsiElement primaryElement, TwigExtensionUsageSymbol symbol) {
        return new TwigExtensionSymbolFindUsagesTarget(primaryElement, symbol);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigExtensionSymbolFindUsagesTarget)) {
            return false;
        }
        TwigExtensionSymbolFindUsagesTarget that = (TwigExtensionSymbolFindUsagesTarget) o;
        return Objects.equals(myPrimaryElement, that.myPrimaryElement) && Objects.equals(mySymbol, that.mySymbol);
    }

    @Override
    public int hashCode() {
        return myPrimaryElement.hashCode() * 31 + mySymbol.hashCode();
    }

    @Override
    public String toString() {
        return "TwigExtensionSymbolFindUsagesTarget(primaryElement=" + myPrimaryElement + ", symbol=" + mySymbol + ")";
    }
}
