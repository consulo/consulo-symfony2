package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;

import java.util.List;

/**
 * Represents a named group of Symfony routes for the Endpoints tool window.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class SymfonyRouteGroup {
    private final Project myProject;
    private final String myName;
    private final List<Route> myRoutes;

    public SymfonyRouteGroup(Project project, String name, List<Route> routes) {
        myProject = project;
        myName = name;
        myRoutes = routes;
    }

    public Project getProject() {
        return myProject;
    }

    public String getName() {
        return myName;
    }

    public List<Route> getRoutes() {
        return myRoutes;
    }
}
