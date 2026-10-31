package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamespaceExtensionParameter {

    private final Project project;

    public TwigNamespaceExtensionParameter(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
