package fr.adrienbrault.idea.symfony2plugin.ui.dict;

import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface UiFilePathInterface {
    boolean exists(Project project);
    void setPath(String path);
    String getPath();

}
