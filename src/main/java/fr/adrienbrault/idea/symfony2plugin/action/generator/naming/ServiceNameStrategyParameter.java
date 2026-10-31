package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceNameStrategyParameter {

    private final Project project;

    private final String className;

    public ServiceNameStrategyParameter(Project project, String className) {
        this.project = project;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Project getProject() {
        return project;
    }

}
