package fr.adrienbrault.idea.symfony2plugin.routing;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerDeprecatedInspection {
    static void hasDeprecatedActionOrClass(Project project, PsiElement element, String text, ProblemsHolder holder) {
        if (RouteHelper.isControllerActionDeprecated(project, text)) {
            holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }
}
