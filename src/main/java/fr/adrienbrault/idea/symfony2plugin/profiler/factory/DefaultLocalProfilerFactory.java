package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.profiler.LocalProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class DefaultLocalProfilerFactory extends LocalProfilerFactory {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(Project project) {
        File csvProfilerFile = getCsvIndex(project);
        if(csvProfilerFile == null) {
            return null;
        }

        return new LocalProfilerIndex(csvProfilerFile, null);
    }

    @Override
    public boolean accepts(Project project) {
        return true;
    }
}
