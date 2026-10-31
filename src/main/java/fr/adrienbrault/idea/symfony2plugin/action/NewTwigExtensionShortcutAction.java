package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * The action in the context menu of the matching directories
 */
@ActionImpl(id = "SymfonyNewTwigExtensionShortcut")
public class NewTwigExtensionShortcutAction extends NewTwigExtensionAction {
    @Override
    public void update(AnActionEvent event) {
        setStatus(
            event,
            Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY)) &&
                NewFileActionUtil.isInGivenDirectoryScope(event, "Twig", "Extension")
        );
    }
}
