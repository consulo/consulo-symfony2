package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import java.util.Map;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewController")
public class NewControllerAction extends AbstractNewPhpClassAction {
    public NewControllerAction() {
        super("Controller", "Create Controller Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Controller";
    }

    @Override
    protected String getTemplateName(Project project, String namespace, PsiDirectory directory) {
        return NewFileActionUtil.guessControllerTemplateType(project);
    }

    @Override
    protected Map<String, String> getExtraVariables(String className, String namespace, PsiDirectory directory) {
        String clazz;
        if (StringUtil.endsWithIgnoreCase(className, "controller")) {
            clazz = className.substring(0, className.length() - "controller".length());
        }
        else {
            clazz = className;
        }

        return Map.of(
            "path", "/" + underscore(clazz).replace("_", "-"),
            "template_path", underscore(clazz)
        );
    }
}
