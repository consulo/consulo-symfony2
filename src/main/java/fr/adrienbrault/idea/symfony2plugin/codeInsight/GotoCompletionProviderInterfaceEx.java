package fr.adrienbrault.idea.symfony2plugin.codeInsight;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface GotoCompletionProviderInterfaceEx extends GotoCompletionProviderInterface {
    /**
     * Extended lookup element implementation
     * allowing resultSet modification
     */
    default void getLookupElements(GotoCompletionProviderLookupArguments arguments) {}
}
