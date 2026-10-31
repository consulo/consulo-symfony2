package fr.adrienbrault.idea.symfony2plugin.javascript;

import consulo.annotation.component.ExtensionImpl;

/**
 * consulo-port: registration of the nested {@link JavascriptCompletionNavigationContributor.GotoDeclaration}
 * (nested classes can not be registered as extensions)
 */
@ExtensionImpl
public class JavascriptCompletionNavigationContributorGotoDeclaration extends JavascriptCompletionNavigationContributor.GotoDeclaration {
}
