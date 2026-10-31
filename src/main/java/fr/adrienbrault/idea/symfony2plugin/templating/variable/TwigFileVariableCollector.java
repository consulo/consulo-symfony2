package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TwigFileVariableCollector {

    default void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {}

    default void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {}
}
