package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * Creates the dedicated Find Usages handler for route declarations.
 * This path is used when Find Usages starts on the declaration PSI itself,
 * for example on #[Route(name: 'home')], on a YAML route key, or on an XML route id.
 * Searches started from usage strings like path('home') or $router->generate('home')
 * are routed through RouteUsageTargetProvider instead.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    /**
     * Activates the custom handler only for supported route declaration PSI.
     */
    @Override
    public boolean canFindUsages(PsiElement element) {
        return RouteUsageUtil.getRouteDeclarationTarget(element) != null;
    }

    /**
     * Normalizes the caret element to the owning route declaration before creating the handler.
     */
    @Override
    public @Nullable FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        PsiElement routeDeclarationTarget = RouteUsageUtil.getRouteDeclarationTarget(element);
        if (routeDeclarationTarget == null) {
            return null;
        }

        return new RouteFindUsagesHandler(routeDeclarationTarget);
    }
}
