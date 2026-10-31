package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewTwigExtension")
public class NewTwigExtensionAction extends AbstractNewPhpClassAction {
    public NewTwigExtensionAction() {
        super("TwigExtension", "Create TwigExtension Class");
    }

    @Override
    protected String getTemplateName(Project project, String namespace, PsiDirectory directory) {
        if (PhpElementsUtil.hasClassOrInterface(project, "\\Twig\\Attribute\\AsTwigFunction")) {
            return "twig_extension_function_attribute";
        }
        else {
            return "twig_extension";
        }
    }
}
