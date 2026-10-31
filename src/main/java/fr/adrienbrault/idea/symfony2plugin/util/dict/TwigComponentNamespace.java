package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigComponentNamespace(String namespace, String templateDirectory, @Nullable String namePrefix) {
}
