package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FilesystemUtil {

    @Nullable
    public static PsiDirectory findParentBundleFolder(PsiDirectory directory) {

        // self click
        if(directory.isDirectory() && directory.getName().endsWith("Bundle")) {
            return directory;
        }

        for (PsiDirectory parent = directory.getParent(); parent != null; parent = parent.getParent()) {
            if(parent.isDirectory() && parent.getName().endsWith("Bundle")) {
                return parent;
            }
        }

        return null;
    }

    /**
     * Try to find an "app" directory on configuration or on project directory in root
     * We also support absolute path in configuration
     */
    public static Collection<VirtualFile> getAppDirectories(Project project) {
        Collection<VirtualFile> virtualFiles = new HashSet<>();

        // find "app" folder on user settings
        String directoryToApp = Settings.getInstance(project).directoryToApp;

        if(FileUtil.isAbsolute(directoryToApp)) {
            // absolute dir given
            VirtualFile fileByIoFile = VirtualFileUtil.findFileByIoFile(new File(directoryToApp), false);
            if(fileByIoFile != null) {
                virtualFiles.add(fileByIoFile);
            }
        } else {
            // relative path resolve
            VirtualFile globalDirectory = VirtualFileUtil.findRelativeFile(
                ProjectUtil.getProjectDir(project),
                directoryToApp.replace("\\", "/").split("/")
            );

            if(globalDirectory != null) {
                virtualFiles.add(globalDirectory);
            }
        }

        // global "app" in root
        VirtualFile templates = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "app");
        if(templates != null) {
            virtualFiles.add(templates);
        }

        return virtualFiles;
    }
}
