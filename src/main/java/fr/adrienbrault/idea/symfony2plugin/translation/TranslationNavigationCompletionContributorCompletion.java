package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of {@link TranslationNavigationCompletionContributor.Completion} (completion.contributor, language PHP);
 * Consulo cannot register nested classes via @ExtensionImpl.
 */
@ExtensionImpl
public class TranslationNavigationCompletionContributorCompletion extends TranslationNavigationCompletionContributor.Completion {
}
