package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import java.util.Map;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewCommand")
public class NewCommandAction extends AbstractNewPhpClassAction {
    public NewCommandAction() {
        super("Command", "Create Command Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Command";
    }

    @Override
    protected String getTemplateName(Project project, String namespace, PsiDirectory directory) {
        return NewFileActionUtil.guessCommandTemplateType(project, namespace);
    }

    @Override
    protected Map<String, String> getExtraVariables(String className, String namespace, PsiDirectory directory) {
        String clazz = className.endsWith("Command") ? className.substring(0, className.length() - "Command".length()) : className;
        String prefix = NewFileActionUtil.getCommandPrefix(directory);
        return Map.of("command_name", prefix + ":" + underscore(clazz));
    }
}
