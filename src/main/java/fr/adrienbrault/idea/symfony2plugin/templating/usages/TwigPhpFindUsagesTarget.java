package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Twig usage target that delegates the search to one or more resolved PHP elements.
 */
public final class TwigPhpFindUsagesTarget implements TwigFindUsagesTarget {
    private final PsiElement myPrimaryElement;
    private final List<PsiElement> myPhpTargets;
    private final List<PsiElement> myPrimaryElements;

    public TwigPhpFindUsagesTarget(PsiElement primaryElement, List<PsiElement> phpTargets) {
        myPrimaryElement = primaryElement;
        myPhpTargets = phpTargets;
        myPrimaryElements = phpTargets;
    }

    @Override
    public PsiElement getPrimaryElement() {
        return myPrimaryElement;
    }

    public List<PsiElement> getPhpTargets() {
        return myPhpTargets;
    }

    @Override
    public List<PsiElement> getPrimaryElements() {
        return myPrimaryElements;
    }

    public PsiElement component1() {
        return myPrimaryElement;
    }

    public List<PsiElement> component2() {
        return myPhpTargets;
    }

    public TwigPhpFindUsagesTarget copy(PsiElement primaryElement, List<PsiElement> phpTargets) {
        return new TwigPhpFindUsagesTarget(primaryElement, phpTargets);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigPhpFindUsagesTarget)) {
            return false;
        }
        TwigPhpFindUsagesTarget that = (TwigPhpFindUsagesTarget) o;
        return Objects.equals(myPrimaryElement, that.myPrimaryElement) && Objects.equals(myPhpTargets, that.myPhpTargets);
    }

    @Override
    public int hashCode() {
        return myPrimaryElement.hashCode() * 31 + myPhpTargets.hashCode();
    }

    @Override
    public String toString() {
        return "TwigPhpFindUsagesTarget(primaryElement=" + myPrimaryElement + ", phpTargets=" + myPhpTargets + ")";
    }
}
