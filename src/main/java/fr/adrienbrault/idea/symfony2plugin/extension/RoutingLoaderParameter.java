package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutingLoaderParameter {

    private final Project project;

    private final Map<String, Route> routes;

    public RoutingLoaderParameter(Project project, Map<String, Route> routes) {
        this.project = project;
        this.routes = routes;
    }

    public Project getProject() {
        return project;
    }

    public void addRoute(Route route) {
        routes.put(route.getName(), route);
    }

    public void addRoutes(Collection<Route> routes) {
        for (Route route : routes) {
            this.routes.put(route.getName(), route);
        }
    }
}
