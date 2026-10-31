package fr.adrienbrault.idea.symfony2plugin.codeInspection;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class InspectionUtil {

    public static void inspectController(PsiElement psiElement, String controllerName, ProblemsHolder holder, final LazyControllerNameResolve lazyControllerNameResolve) {

        List<PsiElement> psiElements = Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), controllerName));
        if(!psiElements.isEmpty()) {
            return;
        }

        ControllerClassOnShortcutReturn shortcutReturn = RouteHelper.getControllerClassOnShortcut(psiElement.getProject(), controllerName);
        if(shortcutReturn == null) {
            return;
        }

        int lastPos = controllerName.lastIndexOf(":") + 1;
        String actionName = controllerName.substring(lastPos);

        final Project project = shortcutReturn.getPhpClass().getProject();
        final String finalActionName = actionName;
        holder.registerProblem(psiElement, "Create Method", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CreateMethodQuickFix(shortcutReturn.getPhpClass(), actionName, (ProblemDescriptor problemDescriptor, PhpClass phpClass, String functionName) -> {

            // attach route parameter inside method
            String parameters = "";
            String routeName = lazyControllerNameResolve.getRouteName();
            if(routeName != null) {
                Collection<Route> routes = RouteHelper.getRoute(project, routeName);
                if(!routes.isEmpty()) {
                    Set<String> vars = routes.iterator().next().getVariables();
                    if(!vars.isEmpty()) {

                        // add dollar char for vars
                        List<String> varsDollar = new ArrayList<>();
                        for(String var: vars) {
                            varsDollar.add("$" + var);
                        }

                        parameters = StringUtils.join(varsDollar, ", ");
                    }
                }
            }

            return new StringBuilder()
                .append("public function ")
                .append(finalActionName)
                .append("(")
                .append(parameters)
                .append(")\n {\n}\n\n");
        }));

    }

    public interface LazyControllerNameResolve {
        @Nullable
        String getRouteName();
    }

}
