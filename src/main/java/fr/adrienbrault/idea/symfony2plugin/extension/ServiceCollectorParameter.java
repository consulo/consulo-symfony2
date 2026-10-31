package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceCollectorParameter {

    public static class Service {

        private final Project project;

        private final Collection<ServiceInterface> services;

        public Service(Project project, Collection<ServiceInterface> services) {
            this.project = project;
            this.services = services;
        }

        public Project getProject() {
            return project;
        }

        public void add(ServiceInterface service) {
            this.services.add(service);
        }

        public void addAll(Collection<ServiceInterface> services) {
            this.services.addAll(services);
        }

        public void add(String id) {
            this.services.add(new SerializableService(id));
        }

        public void add(String id, String clazz) {
            this.services.add(new SerializableService(id).setClassName(clazz));
        }
    }

    public static class Id {

        private final Project project;

        private final Collection<String> ids;

        public Id(Project project, Collection<String> ids) {
            this.project = project;
            this.ids = ids;
        }

        public Project getProject() {
            return project;
        }

        public void add(String id) {
            this.ids.add(id);
        }

        public void addAll(Collection<String> names) {
            this.ids.addAll(names);
        }
    }
}
