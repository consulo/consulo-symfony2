package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 */
public interface GotoCompletionRegistrarParameter {
    void register(ElementPattern<? extends PsiElement> pattern, GotoCompletionContributor contributor);
}
