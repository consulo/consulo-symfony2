package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewCompilerPass")
public class NewCompilerPassAction extends AbstractNewPhpClassAction {
    public NewCompilerPassAction() {
        super("CompilerPass", "Create CompilerPass Class");
    }

    @Override
    protected String getTemplateName(Project project, String namespace, PsiDirectory directory) {
        return "compiler_pass";
    }
}
