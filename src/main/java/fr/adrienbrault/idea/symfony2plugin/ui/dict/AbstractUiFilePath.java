package fr.adrienbrault.idea.symfony2plugin.ui.dict;

import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public abstract class AbstractUiFilePath implements UiFilePathInterface {

    protected String path;

    public boolean exists(Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            return VirtualFileUtil.findRelativeFile(this.path, ProjectUtil.getProjectDir(project)) != null;
        }

        return new File(this.path).exists();
    }

    @Attribute("path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


}
