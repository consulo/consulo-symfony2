package fr.adrienbrault.idea.symfony2plugin.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteUrlMatcherSymbolContributor implements GotoSymbolContributor {
    // TODO consulo-port: ChooseByNameContributorEx2 missing, never called
    public void processNames(Processor<String> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        String name = parameters.getLocalPatternName();
        if (RouteHelper.hasRoutesForPathWithPlaceholderMatch(project, name)) {
            processor.process(name);
        }
    }

    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        Set<PsiElement> targets = new HashSet<>();
        for (Pair<Route, PsiElement> pair : RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, name)) {
            Route route = pair.getFirst();
            PsiElement target = pair.getSecond();
            if (route.getPath() == null || !targets.add(target)) {
                continue;
            }

            processor.process(
                NavigationItemPresentableOverwrite.create(
                    target,
                    route.getPathPresentable(),
                    Symfony2Icons.ROUTE,
                    "Symfony Route",
                    true,
                    name
                )
            );
        }
    }
}
