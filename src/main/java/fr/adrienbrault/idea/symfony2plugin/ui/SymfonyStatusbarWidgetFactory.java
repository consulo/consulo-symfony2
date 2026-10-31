package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.widget.SymfonyProfilerWidget;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(id = "symfony.status.bar")
public class SymfonyStatusbarWidgetFactory implements StatusBarWidgetFactory {
    @Override
    public String getId() {
        return "symfony.status.bar";
    }

    @Override
    public String getDisplayName() {
        return "Symfony";
    }

    @Override
    public boolean isAvailable(Project project) {
        return Symfony2ProjectComponent.isEnabled(project);
    }

    @Override
    public StatusBarWidget createWidget(Project project) {
        return new SymfonyProfilerWidget(project, this);
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return true;
    }
}
