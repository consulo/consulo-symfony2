package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * Describes the effective identity of a Twig Find Usages session.
 */
public interface TwigFindUsagesTarget {
    /**
     * Anchor PSI element used to create the handler and open the Usage View.
     */
    PsiElement getPrimaryElement();

    /**
     * Effective search targets exposed to the Usage View, either PHP delegates or the Twig symbol itself.
     */
    List<PsiElement> getPrimaryElements();
}
