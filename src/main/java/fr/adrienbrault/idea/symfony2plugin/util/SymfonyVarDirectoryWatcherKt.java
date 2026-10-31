package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * Top-level functions of {@code SymfonyVarDirectoryWatcher.kt}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class SymfonyVarDirectoryWatcherKt {
    private SymfonyVarDirectoryWatcherKt() {
    }

    /**
     * Retrieves the {@link SymfonyVarDirectoryWatcher} project service.
     */
    public static SymfonyVarDirectoryWatcher getSymfonyVarDirectoryWatcher(Project project) {
        return project.getInstance(SymfonyVarDirectoryWatcher.class);
    }

    /**
     * Syncs watcher lifecycle with the Symfony module extension state of the project.
     */
    public static void syncSymfonyVarDirectoryWatcher(Project project) {
        if (Symfony2ProjectComponent.isEnabled(project)) {
            getSymfonyVarDirectoryWatcher(project).reloadConfiguration();
            return;
        }

        SymfonyVarDirectoryWatcher watcher = project.getInstanceIfCreated(SymfonyVarDirectoryWatcher.class);
        if (watcher != null) {
            watcher.reloadConfiguration();
        }
    }
}
