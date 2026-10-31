package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.annotation.component.ExtensionImpl;

/**
 * consulo-port: registration of the nested {@link TwigComponentHtmlTagExtensions.TwigTemplateXmlExtension};
 * a nested class cannot carry @ExtensionImpl
 */
@ExtensionImpl(id = "TwigTemplateXmlExtension")
public class TwigTemplateXmlExtensionImpl extends TwigComponentHtmlTagExtensions.TwigTemplateXmlExtension {
}
