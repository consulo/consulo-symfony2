package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ExtensionImpl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DoctrineModelProvider {
    Collection<DoctrineModelProviderParameter.DoctrineModel> collectModels(DoctrineModelProviderParameter parameter);
}
