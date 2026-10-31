package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    private final String missingRoute;

    public RouteGuessTypoQuickFix(String missingRoute) {
        this.missingRoute = missingRoute;
    }

    @Override
    protected String getSuggestionLabel() {
        return "Route";
    }

    @Override
    protected List<String> getSimilarItems(Project project) {
        return SimilarSuggestionUtil.findSimilarString(missingRoute, RouteHelper.getAllRoutes(project).keySet());
    }
}
