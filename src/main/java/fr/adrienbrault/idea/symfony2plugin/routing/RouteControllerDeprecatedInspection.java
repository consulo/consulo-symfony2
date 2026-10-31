package fr.adrienbrault.idea.symfony2plugin.routing;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

/**
 * consulo-port: the registered nested inspections RouteControllerDeprecatedInspection$RouteControllerDeprecatedXmlLocalInspectionTool /
 * $RouteControllerDeprecatedYamlLocalInspectionTool moved to the top-level classes {@link RouteControllerDeprecatedXmlLocalInspectionTool}
 * and {@link RouteControllerDeprecatedYamlLocalInspectionTool} (nested classes cannot carry @ExtensionImpl).
 * The former private top-level function of the Kotlin file lives here.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerDeprecatedInspection {
    static void hasDeprecatedActionOrClass(Project project, PsiElement element, String text, ProblemsHolder holder) {
        if (RouteHelper.isControllerActionDeprecated(project, text)) {
            holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }
}
