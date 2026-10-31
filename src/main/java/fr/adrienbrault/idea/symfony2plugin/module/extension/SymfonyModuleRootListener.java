package fr.adrienbrault.idea.symfony2plugin.module.extension;

import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import jakarta.inject.Inject;

/**
 * Enabling / disabling the Symfony module extension or changing its settings is a module roots change;
 * keeps the var directory watcher in sync with it.
 */
@TopicImpl(ComponentScope.PROJECT)
public class SymfonyModuleRootListener implements ModuleRootListener {
    private final Project myProject;

    @Inject
    public SymfonyModuleRootListener(Project project) {
        myProject = project;
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
        if (myProject.isDisposed() || myProject.isDefault()) {
            return;
        }

        SymfonyVarDirectoryWatcherKt.syncSymfonyVarDirectoryWatcher(myProject);

        if (Symfony2ProjectComponent.isEnabled(myProject)) {
            IdeHelper.expireEnableMessage(myProject);
        }
    }
}
