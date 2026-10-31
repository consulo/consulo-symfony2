package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Extract Twig path of config.yml
 *
 *  twig:
 *    paths:
 *      "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
 *      "%kernel.project_dir%/src/vendor/bundle/Resources/views": core
 *      "%kernel.project_dir%/src/vendor/bundle/Resources/views": !core
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after JsonFileIndexTwigNamespaces")
public class ConfigAddPathTwigNamespaces implements TwigNamespaceExtension {
    private static final Key<CachedValue<Collection<Pair<String, String>>>> CACHE = Key.create("TWIG_CONFIG_ADD_PATH_CACHE");

    @Override
    public Collection<TwigPath> getNamespaces(TwigNamespaceExtensionParameter parameter) {
        Project project = parameter.getProject();

        Collection<Pair<String, String>> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(getTwigPaths(project), FileIndexCaches.getModificationTrackerForIndexId(project, ConfigStubIndex.KEY)),
            false
        );

        // TwigPath is not cache able as it right now; we need to build it here
        return cachedValue.stream()
            .map(p -> new TwigPath(p.getFirst(), p.getSecond(), TwigUtil.NamespaceType.ADD_PATH, true))
            .collect(Collectors.toList());
    }

    private static Collection<Pair<String, String>> getTwigPaths(Project project) {
        Collection<Pair<String, String>> twigPathNamespace = new HashSet<>();

        // file config files a eg ".../app/..." or "../packages/..."
        Collection<PsiFile> psiFiles = PsiElementUtils.convertVirtualFilesToPsiFiles(
            project,
            ConfigUtil.getConfigurations(project, "twig")
        );

        for (PsiFile psiFile : psiFiles) {
            if (!(psiFile instanceof YAMLFile)) {
                continue;
            }

            for (Pair<String, String> stringStringPair : TwigUtil.getTwigPathFromYamlConfigResolved((YAMLFile) psiFile)) {
                // default path
                String first = stringStringPair.getFirst();
                if(first == null || first.isEmpty()) {
                    first = TwigUtil.MAIN;
                }

                twigPathNamespace.add(Pair.create(stringStringPair.getSecond(), first));
            }
        }

        return twigPathNamespace;
    }
}
