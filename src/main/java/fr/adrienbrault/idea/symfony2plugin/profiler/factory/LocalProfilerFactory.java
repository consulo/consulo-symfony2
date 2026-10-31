package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import consulo.project.Project;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.LocalProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerFactory implements ProfilerFactoryInterface {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(Project project) {
        File csvIndex = findCsvProfilerFile(project);
        if (csvIndex == null) {
            return null;
        }

        String profilerLocalUrl = Settings.getInstance(project).profilerLocalUrl;

        // fine: user defined base url use this one
        // else no profiler given let profiler try to find url
        String profilerUrl = null;
        if (StringUtils.isNotBlank(profilerLocalUrl)) {
            profilerUrl = profilerLocalUrl;
        }

        return new LocalProfilerIndex(csvIndex, profilerUrl);
    }

    /**
     * find csv on settings or container configuration
     */
    @Nullable
    private File findCsvProfilerFile(Project project) {
        String profilerCsvPath = Settings.getInstance(project).profilerCsvPath;
        if (StringUtils.isBlank(profilerCsvPath)) {
            return getCsvIndex(project);
        }

        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), profilerCsvPath.replace("\\", "/").split("/"));
        if (relativeFile != null) {
            return VirtualFileUtil.virtualToIoFile(relativeFile);
        }

        return null;
    }

    @Nullable
    protected File getCsvIndex(Project project) {
        for (VirtualFile file : Symfony2ProjectComponent.getContainerFiles(project)) {
            VirtualFile parent = file.getParent();
            if (parent == null) {
                continue;
            }

            VirtualFile csvFile = VirtualFileUtil.findRelativeFile(parent, "profiler", "index.csv");
            if (csvFile != null && csvFile.exists()) {
                return VirtualFileUtil.virtualToIoFile(csvFile);
            }
        }

        return null;
    }

    @Override
    public boolean accepts(Project project) {
        return Settings.getInstance(project).profilerLocalEnabled;
    }
}
