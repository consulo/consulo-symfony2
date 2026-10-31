package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewKernelTestCase")
public class NewKernelTestCaseAction extends AbstractNewPhpClassAction {
    public NewKernelTestCaseAction() {
        super("KernelTestCase", "Create KernelTestCase Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Test";
    }

    @Override
    protected String getTemplateName(Project project, String namespace, PsiDirectory directory) {
        return "kernel_test_case";
    }
}
