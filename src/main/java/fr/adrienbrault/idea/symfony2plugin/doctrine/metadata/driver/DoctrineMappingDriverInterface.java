package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineMappingDriverInterface {
    DoctrineMetadataModel getMetadata(DoctrineMappingDriverArguments arguments);
}
