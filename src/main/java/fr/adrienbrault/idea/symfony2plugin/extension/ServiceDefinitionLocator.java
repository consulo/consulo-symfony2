package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ServiceDefinitionLocator {

    /**
     * Find service declaration by id
     */
    void locate(String serviceId, ServiceDefinitionLocatorParameter parameter);
}
