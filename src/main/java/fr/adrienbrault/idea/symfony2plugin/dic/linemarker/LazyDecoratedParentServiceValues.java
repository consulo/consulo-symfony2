package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Proxy to load decorated services lazily
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class LazyDecoratedParentServiceValues {
    private final Project project;

    @Nullable
    private Map<String, Collection<ContainerService>> decorates;

    @Nullable
    private Map<String, Collection<ContainerService>> parents;

    LazyDecoratedParentServiceValues(Project project) {
        this.project = project;
    }

    public Map<String, Collection<ContainerService>> getDecoratedServices() {
        if(this.decorates == null) {
            this.decorates = ServiceIndexUtil.getDecoratedServices(this.project);
        }

        return decorates;
    }

    public Map<String, Collection<ContainerService>> getParentServices() {
        if(this.parents == null) {
            this.parents = ServiceIndexUtil.getParentServices(this.project);
        }

        return parents;
    }
}
