package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.annotation.component.ExtensionImpl;

/**
 * consulo-port: registration of the nested {@link TwigComponentHtmlTagExtensions.TwigTemplateTagNameProvider};
 * a nested class cannot carry @ExtensionImpl
 */
@ExtensionImpl
public class TwigTemplateTagNameProviderImpl extends TwigComponentHtmlTagExtensions.TwigTemplateTagNameProvider {
}
