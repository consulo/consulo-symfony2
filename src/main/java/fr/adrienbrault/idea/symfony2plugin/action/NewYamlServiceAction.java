package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyNewYamlService")
public class NewYamlServiceAction extends AbstractProjectDumbAwareAction {
    public NewYamlServiceAction() {
        super(
            "Yaml Service",
            "Create new Yaml File",
            YAMLFileType.YML.getIcon() // TODO consulo-port: no AllIcons.Nodes.DataTables icon
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
