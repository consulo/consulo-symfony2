package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 */
public interface GotoCompletionContributor {
    @Nullable
    GotoCompletionProvider getProvider(PsiElement psiElement);
}
