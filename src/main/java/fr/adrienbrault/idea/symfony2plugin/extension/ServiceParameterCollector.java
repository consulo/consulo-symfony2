package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ServiceParameterCollector {
    /**
     * Warning expect high traffic, collector needs to be highly optimized
     */
    void collectIds(ServiceParameterCollectorParameter.Id parameter);
}
