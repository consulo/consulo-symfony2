package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.jetbrains.twig.elements.TwigBlockStatement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigBlockEmbed(String templateName, String blockName, TwigBlockStatement target) {
}
