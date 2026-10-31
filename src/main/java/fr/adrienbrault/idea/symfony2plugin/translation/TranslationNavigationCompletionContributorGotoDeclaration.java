package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of {@link TranslationNavigationCompletionContributor.GotoDeclaration} (gotoDeclarationHandler);
 * Consulo cannot register nested classes via @ExtensionImpl.
 */
@ExtensionImpl
public class TranslationNavigationCompletionContributorGotoDeclaration extends TranslationNavigationCompletionContributor.GotoDeclaration {
}
