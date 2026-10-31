package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceParameterCollectorParameter {
    public static class Id {

        private final Project project;

        private final Collection<ContainerParameter> ids;

        public Id(Project project, Collection<ContainerParameter> ids) {
            this.project = project;
            this.ids = ids;
        }

        public Project getProject() {
            return project;
        }

        public void add(ContainerParameter id) {
            this.ids.add(id);
        }

        public void addAll(Collection<ContainerParameter> names) {
            this.ids.addAll(names);
        }
    }
}
