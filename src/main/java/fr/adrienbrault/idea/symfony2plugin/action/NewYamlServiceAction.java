package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewYamlService")
public class NewYamlServiceAction extends AbstractProjectDumbAwareAction {
    public NewYamlServiceAction() {
        super(
            "Yaml Service",
            "Create new Yaml File",
            PlatformIconGroup.nodesDatatables()
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

        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.yml");
    }
}
