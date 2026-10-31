package fr.adrienbrault.idea.symfony2plugin.twig.loader;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileImplementsLazyLoader {
    private final Project project;

    private final VirtualFile virtualFile;

    private @Nullable Collection<VirtualFile> files;

    public FileImplementsLazyLoader(Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
    }

    public Collection<VirtualFile> getFiles() {
        if(files != null) {
            return files;
        }

        return files = TwigUtil.getTemplatesExtendingFile(project, virtualFile);
    }
}
