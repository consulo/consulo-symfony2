package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerFactoryInterface {
    @Nullable
    ProfilerIndexInterface createProfilerIndex(Project project);

    boolean accepts(Project project);
}
