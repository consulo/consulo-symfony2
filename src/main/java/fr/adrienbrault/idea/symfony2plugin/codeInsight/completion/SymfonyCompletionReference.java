package fr.adrienbrault.idea.symfony2plugin.codeInsight.completion;

import consulo.language.psi.PsiReference;

/**
 * A reference of the plugin which completes its names - routes, services, templates, ... -
 * through {@link SymfonyReferenceCompletionContributor} and not through the legacy {@link PsiReference#getVariants()}.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
public interface SymfonyCompletionReference extends PsiReference {
    /**
     * @return {@link consulo.language.editor.completion.lookup.LookupElement}s or names, like {@link PsiReference#getVariants()}
     */
    Object[] getCompletionVariants();
}
