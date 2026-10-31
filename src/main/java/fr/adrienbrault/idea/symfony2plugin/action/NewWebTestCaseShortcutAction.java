package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * The action in the context menu of the matching directories
 */
@ActionImpl(id = "SymfonyNewWebTestCaseShortcut")
public class NewWebTestCaseShortcutAction extends NewWebTestCaseAction {
    @Override
    public void update(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        PsiDirectory directory = NewFileActionUtil.getSelectedDirectoryFromAction(event);
        setStatus(
            event,
            project != null &&
                Symfony2ProjectComponent.isEnabled(project) &&
                directory != null &&
                ProjectRootsUtil.isInTestSource(directory.getVirtualFile(), project)
        );
    }
}
