package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigTypeResolver {
    void resolve(Project project, Collection<TwigTypeContainer> targets, @Nullable Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables);
}
