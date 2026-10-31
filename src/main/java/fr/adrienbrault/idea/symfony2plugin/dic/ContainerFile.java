package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.util.xml.serializer.annotation.Tag;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.AbstractUiFilePath;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("container_file")
public class ContainerFile extends AbstractUiFilePath {

    public ContainerFile() {
    }

    public ContainerFile(String path) {
        this.path = path;
    }

    @Nullable
    public VirtualFile getVirtualFile(Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            return VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), this.path.replace("\\", "/").split("/"));
        }

        VirtualFile virtualFile = VirtualFileUtil.findFileByIoFile(new java.io.File(this.path), false);
        if (virtualFile == null || !virtualFile.exists()) {
            return null;
        }

        return virtualFile;
    }
}
