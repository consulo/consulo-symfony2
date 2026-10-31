package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewXmlService")
public class NewXmlServiceAction extends AbstractProjectDumbAwareAction {
    public NewXmlServiceAction() {
        super(
            "Xml Service",
            "Create new Xml File",
            PlatformIconGroup.filetypesXml()
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

        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.xml");
    }
}
