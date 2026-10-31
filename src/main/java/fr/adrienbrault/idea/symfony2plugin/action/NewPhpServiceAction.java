package fr.adrienbrault.idea.symfony2plugin.action;

import com.jetbrains.php.PhpIcons;
import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewPhpService")
public class NewPhpServiceAction extends AbstractProjectDumbAwareAction {
    public NewPhpServiceAction() {
        super(
            "PHP Service",
            "Create new PHP File",
            PhpIcons.PHP_FILE
        );
    }

    @Override
    public void update(AnActionEvent event) {
        setStatus(
            event,
            Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY)) &&
                NewFileActionUtil.getSelectedDirectoryFromAction(event) != null
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.php");
    }
}
