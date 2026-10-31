package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.annotation.component.ExtensionImpl;

/**
 * consulo-port: registration of the nested {@link TemplateRenderBlockCompletionContributor.GotoDeclaration};
 * a nested class cannot carry @ExtensionImpl
 */
@ExtensionImpl
public class TemplateRenderBlockCompletionContributorGotoDeclaration extends TemplateRenderBlockCompletionContributor.GotoDeclaration {
}
