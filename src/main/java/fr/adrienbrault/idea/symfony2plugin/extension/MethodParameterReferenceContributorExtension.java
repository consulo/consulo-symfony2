package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface MethodParameterReferenceContributorExtension {
    Collection<MethodParameterSetting> getSettings(MethodParameterReferenceContributorParameter parameter);
}
