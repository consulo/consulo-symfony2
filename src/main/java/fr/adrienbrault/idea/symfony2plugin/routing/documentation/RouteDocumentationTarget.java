package fr.adrienbrault.idea.symfony2plugin.routing.documentation;

import consulo.application.Application;
import consulo.component.util.pointer.Pointer;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationService;
import consulo.navigation.TargetPresentation;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.RouteDocumentationTargetProvider.RouteDocumentationContext;
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTargetProvider;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * TODO consulo-port: platform.backend.documentation API missing (DocumentationTarget, DocumentationResult; could become
 *  LanguageDocumentationProvider.getCustomDocumentationElement + generateDoc). Was "implements DocumentationTarget";
 *  the former overrides are kept as plain methods, computeDocumentation() (DocumentationResult) is dropped.
 */
public class RouteDocumentationTarget {
    private final RouteDocumentationContext context;

    public RouteDocumentationTarget(RouteDocumentationContext context) {
        this.context = context;
    }

    public Pointer<RouteDocumentationTarget> createPointer() {
        SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(context.getAnchor());

        return () -> {
            PsiElement anchor = pointer.getElement();
            if (anchor == null) {
                return null;
            }

            RouteDocumentationContext restored = new RouteDocumentationTargetProvider().resolveContext(anchor.getContainingFile(), anchor.getTextOffset());
            if (restored == null) {
                return null;
            }

            return new RouteDocumentationTarget(restored);
        };
    }

    public @Nullable Navigatable getNavigatable() {
        PsiElement anchor = context.getAnchor();
        if (!anchor.isValid() || DumbService.isDumb(anchor.getProject())) {
            return null;
        }

        Collection<PsiElement> targets = RouteHelper.getRouteNameTarget(anchor.getProject(), context.getRouteName());
        if (targets.size() != 1) {
            return null;
        }

        PsiElement navigationElement = targets.iterator().next().getNavigationElement();
        return navigationElement instanceof Navigatable navigatable ? navigatable : null;
    }

    public TargetPresentation computePresentation() {
        Navigatable navigatable = getNavigatable();
        String locationText = null;
        if (navigatable instanceof PsiElement psiElement) {
            PsiFile containingFile = psiElement.getContainingFile();
            if (containingFile != null) {
                locationText = containingFile.getName();
            }
        }

        // TODO consulo-port: TargetPresentation.builder(..) -> NavigationService.presentationBuilder(..); locationText null -> empty
        return Application.get().getInstance(NavigationService.class)
            .presentationBuilder(LocalizeValue.of(context.getRouteName()))
            .withIcon(Symfony2Icons.ROUTE)
            .withLocationText(LocalizeValue.ofNullable(locationText))
            .build();
    }

    public @Nullable String computeDocumentationHint() {
        return documentation();
    }

    private @Nullable String documentation() {
        PsiElement anchor = context.getAnchor();
        if (!anchor.isValid()) {
            return null;
        }

        RouteDocumentationContext current = new RouteDocumentationTargetProvider().resolveContext(anchor.getContainingFile(), anchor.getTextOffset());
        if (current == null) {
            return null;
        }

        Collection<Route> routes = RouteHelper.getRoute(anchor.getProject(), current.getRouteName());

        if (routes.isEmpty()) {
            return null;
        }

        int twigTemplates = FileBasedIndex.getInstance().getContainingFiles(
            TwigRouteUsageStubIndex.KEY, current.getRouteName(), GlobalSearchScope.projectScope(anchor.getProject())
        ).size();

        return RouteDocumentationTargetProviderKt.renderRouteDocumentation(routes, twigTemplates);
    }

    public @Nullable DataContext findUsagesContext() {
        PsiElement anchor = context.getAnchor();
        if (!anchor.isValid()) {
            return null;
        }

        RouteDocumentationContext current = new RouteDocumentationTargetProvider().resolveContext(anchor.getContainingFile(), anchor.getTextOffset());
        if (current == null) {
            return null;
        }

        UsageTarget[] targets = new RouteUsageTargetProvider().getTargets(current.getAnchor());
        if (targets == null || targets.length == 0) {
            return null;
        }

        return DataContext.builder()
            .add(Project.KEY, anchor.getProject())
            .add(PsiElement.KEY, current.getAnchor())
            .add(PsiFile.KEY, current.getAnchor().getContainingFile())
            .add(UsageView.USAGE_TARGETS_KEY, targets)
            .build();
    }
}
