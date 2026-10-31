package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TwigNamespaceExtension {
    Collection<TwigPath> getNamespaces(TwigNamespaceExtensionParameter parameter);
}
