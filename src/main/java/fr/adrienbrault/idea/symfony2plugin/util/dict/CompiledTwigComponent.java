package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jspecify.annotations.Nullable;

/**
 * Symfony UX TwigComponent metadata extracted from compiled container XML.
 */
public record CompiledTwigComponent(
    String name,
    @Nullable String phpClass,
    @Nullable String template,
    @Nullable String templateFromMethod
) {
}
