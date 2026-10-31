package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import consulo.project.DumbService;
import com.jetbrains.php.lang.PhpLanguage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.component.util.ModificationTracker;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointType;
import consulo.endpoint.FrameworkPresentation;
import consulo.endpoint.presentation.HttpMethodPresentation;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exposes Symfony routes in IntelliJ Ultimate's Endpoints tool window.
 *
 * Routes are exposed in a single group to avoid imposing project-specific naming conventions.
 * Internal routes starting with "_" are excluded.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/endpoints-api.html">Endpoints API</a>
 */
@ExtensionImpl
public class SymfonyEndpointProvider implements EndpointProvider<SymfonyRouteGroup, Route> {
    private static final String ROUTES_GROUP_NAME = "routes";

    private static final FrameworkPresentation PRESENTATION = new FrameworkPresentation(
        "symfony",
        "Symfony",
        Symfony2Icons.SYMFONY
    );

    private final Project myProject;

    @Inject
    public SymfonyEndpointProvider(Project project) {
        myProject = project;
    }

    @Override
    public EndpointType getEndpointType() {
        return EndpointType.HTTP_SERVER_TYPE;
    }

    @Override
    public FrameworkPresentation getPresentation() {
        return PRESENTATION;
    }

    @Override
    public Status getStatus() {
        if (!Symfony2ProjectComponent.isEnabled(myProject)) {
            return Status.UNAVAILABLE;
        }
        // the routes come from indexes, which are not ready while indexing
        if (DumbService.isDumb(myProject)) {
            return Status.AVAILABLE;
        }

        Map<String, Route> routes = ReadAction.compute(() -> RouteHelper.getAllRoutesUnique(myProject));
        return routes.isEmpty() ? Status.AVAILABLE : Status.HAS_ENDPOINTS;
    }

    @RequiredReadAction
    @Override
    public Iterable<SymfonyRouteGroup> getEndpointGroups(EndpointFilter filter) {
        if (!Symfony2ProjectComponent.isEnabled(myProject) || DumbService.isDumb(myProject)) {
            return Collections.emptyList();
        }

        Map<String, Route> allRoutes = RouteHelper.getAllRoutesUnique(myProject);
        List<Route> routes = new ArrayList<>();

        for (Route route : allRoutes.values()) {
            // Skip internal Symfony framework routes
            if (route.getName().startsWith("_")) {
                continue;
            }

            // Skip routes without a path
            String path = route.getPath();
            if (path == null || path.isEmpty()) {
                continue;
            }

            routes.add(route);
        }

        if (routes.isEmpty()) {
            return Collections.emptyList();
        }

        return List.of(new SymfonyRouteGroup(myProject, ROUTES_GROUP_NAME, routes));
    }

    @Override
    public Iterable<Route> getEndpoints(SymfonyRouteGroup group) {
        return group.getRoutes();
    }

    @Override
    public boolean isValidEndpoint(SymfonyRouteGroup group, Route endpoint) {
        String path = endpoint.getPath();
        return path != null && !path.isEmpty();
    }

    @Override
    public ItemPresentation getEndpointPresentation(SymfonyRouteGroup group, Route endpoint) {
        String path = endpoint.getPath();
        Collection<String> methods = endpoint.getMethods();
        String definitionSource = buildDefinitionSource(endpoint);

        if (methods.isEmpty()) {
            return new HttpMethodPresentation(path, (String) null, definitionSource, Symfony2Icons.SYMFONY);
        }
        else {
            return new HttpMethodPresentation(path, new ArrayList<>(methods), definitionSource, Symfony2Icons.SYMFONY);
        }
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNavigationElement(SymfonyRouteGroup group, Route endpoint) {
        PsiElement[] methods = RouteHelper.getMethods(group.getProject(), endpoint.getName());
        return methods.length > 0 ? methods[0] : null;
    }

    @Override
    public ModificationTracker getModificationTracker() {
        // FileBasedIndex#getIndexModificationStamp can assert on EDT in Endpoints scrolling/update paths.
        // Use PSI tracker to keep Endpoints refresh safe on EDT.
        return PsiModificationTracker.getInstance(myProject).forLanguage(PhpLanguage.INSTANCE);
    }

    private static String buildDefinitionSource(Route endpoint) {
        String routeName = endpoint.getName();
        String controller = endpoint.getController();

        if (controller == null || controller.isEmpty()) {
            return routeName;
        }

        return routeName + " (" + formatControllerShort(controller) + ")";
    }

    private static String formatControllerShort(String controller) {
        if (controller.contains("::")) {
            String[] parts = controller.split("::", 2);
            return shortClassName(parts[0]) + ":" + parts[1];
        }

        int colonIndex = controller.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < controller.length() - 1) {
            String left = controller.substring(0, colonIndex);
            String right = controller.substring(colonIndex + 1);
            return shortClassName(left) + ":" + right;
        }

        return shortClassName(controller);
    }

    private static String shortClassName(String value) {
        int namespaceSeparator = value.lastIndexOf('\\');
        if (namespaceSeparator >= 0 && namespaceSeparator < value.length() - 1) {
            return value.substring(namespaceSeparator + 1);
        }
        return value;
    }
}
