package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.annotation.component.ExtensionImpl;

/**
 * consulo-port: registration of the nested {@link TwigComponentHtmlTagExtensions.TwigTemplateXmlSuppressionProvider};
 * a nested class cannot carry @ExtensionImpl
 */
@ExtensionImpl
public class TwigTemplateXmlSuppressionProviderImpl extends TwigComponentHtmlTagExtensions.TwigTemplateXmlSuppressionProvider {
}
