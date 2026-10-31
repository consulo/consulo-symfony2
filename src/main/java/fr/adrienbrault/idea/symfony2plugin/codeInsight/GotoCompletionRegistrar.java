package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GotoCompletionRegistrar {
    void register(GotoCompletionRegistrarParameter registrar);
}
