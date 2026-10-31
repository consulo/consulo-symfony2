package fr.adrienbrault.idea.symfony2plugin.util.dict;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleFile {

    private final SymfonyBundle symfonyBundle;
    private final VirtualFile virtualFile;
    private final Project project;

    public BundleFile(SymfonyBundle symfonyBundle, VirtualFile virtualFile, Project project) {
        this.symfonyBundle = symfonyBundle;
        this.virtualFile = virtualFile;
        this.project = project;
    }

    public SymfonyBundle getSymfonyBundle() {
        return symfonyBundle;
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    public @Nullable String getShortcutPath() {
        return this.getSymfonyBundle().getFileShortcut(this);
    }

    public Project getProject() {
        return project;
    }

}
