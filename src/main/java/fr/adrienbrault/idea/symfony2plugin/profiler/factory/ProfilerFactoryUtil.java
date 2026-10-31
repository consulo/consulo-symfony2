package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerFactoryUtil {
    private static final ProfilerFactoryInterface[] PROFILER = new ProfilerFactoryInterface[] {
        new HttpProfilerFactory(),
        new LocalProfilerFactory(),
    };

    @Nullable
    public static ProfilerIndexInterface createIndex(Project project) {
        for (ProfilerFactoryInterface factory : PROFILER) {
            if(!factory.accepts(project)) {
                continue;
            }

            ProfilerIndexInterface profiler = factory.createProfilerIndex(project);
            if(profiler != null) {
                return profiler;
            }
        }

        // non user setting try to use local filesystem with self searching path
        return new DefaultLocalProfilerFactory().createProfilerIndex(project);
    }
}
