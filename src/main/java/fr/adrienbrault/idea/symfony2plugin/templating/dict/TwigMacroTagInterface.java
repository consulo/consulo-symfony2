package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jspecify.annotations.Nullable;

/**
 * Dict for "{% macro input(name, value, type, size) %}"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigMacroTagInterface {
    String name();

    @Nullable
    String parameters();
}
