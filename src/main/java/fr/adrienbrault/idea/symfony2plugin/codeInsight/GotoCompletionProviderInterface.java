package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionProviderInterface {
    Collection<LookupElement> getLookupElements();

    Collection<PsiElement> getPsiTargets(PsiElement element);
}
