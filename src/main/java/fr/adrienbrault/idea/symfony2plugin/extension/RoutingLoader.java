package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RoutingLoader {
    void invoke(RoutingLoaderParameter parameter);
}
