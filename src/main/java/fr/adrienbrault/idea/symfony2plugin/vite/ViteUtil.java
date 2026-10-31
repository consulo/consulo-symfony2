package fr.adrienbrault.idea.symfony2plugin.vite;

import com.intellij.lang.javascript.psi.JSFile;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Utility for collecting Vite entry points from vite.config.js / vite.config.ts files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class ViteUtil {
    public static final ViteUtil INSTANCE = new ViteUtil();

    private static final Key<CachedValue<List<ViteEntry>>> VITE_CACHE_KEY = Key.create("SYMFONY_VITE_ENTRIES");

    private ViteUtil() {
    }

    public List<ViteEntry> getEntries(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            VITE_CACHE_KEY,
            () -> {
                Collection<VirtualFile> configFiles = getViteConfigFiles(project);
                List<ViteEntry> entries = collectEntries(project, configFiles);
                Object[] dependencies;
                if (configFiles.isEmpty()) {
                    dependencies = new Object[]{project};
                }
                else {
                    dependencies = configFiles.toArray();
                }
                return CachedValueProvider.Result.create(entries, dependencies);
            },
            false
        );
    }

    private List<ViteEntry> collectEntries(Project project, Collection<VirtualFile> configFiles) {
        List<ViteEntry> entries = new ArrayList<>();

        for (VirtualFile file : configFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof JSFile jsFile) {
                entries.addAll(ViteConfigParser.INSTANCE.parseEntries(jsFile));
            }
        }

        return entries;
    }

    public Collection<VirtualFile> getViteConfigFiles(Project project) {
        List<VirtualFile> files = new ArrayList<>();

        files.addAll(FilenameIndex.getVirtualFilesByName(project, "vite.config.js", GlobalSearchScope.projectScope(project)));
        files.addAll(FilenameIndex.getVirtualFilesByName(project, "vite.config.ts", GlobalSearchScope.projectScope(project)));

        List<VirtualFile> result = new ArrayList<>();
        for (VirtualFile file : files) {
            if (!isTestFile(project, file)) {
                result.add(file);
            }
        }

        return result;
    }

    private boolean isTestFile(Project project, VirtualFile virtualFile) {
        String path = VirtualFileUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
        if (path == null) {
            return false;
        }

        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("/test/") || lower.contains("/tests/");
    }
}
