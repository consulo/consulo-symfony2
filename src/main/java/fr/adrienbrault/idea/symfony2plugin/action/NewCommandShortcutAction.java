package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import java.util.Map;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * The action in the context menu of the matching directories
 */
@ActionImpl(id = "SymfonyNewCommandShortcut")
public class NewCommandShortcutAction extends NewCommandAction {
    @Override
    public void update(AnActionEvent event) {
        setStatus(
            event,
            Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY)) &&
                NewFileActionUtil.isInGivenDirectoryScope(event, "Command")
        );
    }
}
