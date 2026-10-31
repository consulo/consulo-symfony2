package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigBlockSymbolContributor implements GotoSymbolContributor {
    private static final Key<CachedValue<String[]>> SYMFONY_TWIG_BLOCK_NAMES = Key.create("SYMFONY_TWIG_BLOCK_NAMES");

    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = scope instanceof ProjectAwareSearchScope projectAwareScope ? projectAwareScope.getProject() : null;
        if (project == null) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        String[] blocks = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TWIG_BLOCK_NAMES,
            () -> {
                Set<String> blockNames = new LinkedHashSet<>();
                for (Set<String> values : FileBasedIndex.getInstance()
                    .getValues(TwigBlockIndexExtension.KEY, "block", GlobalSearchScope.allScope(project))) {
                    blockNames.addAll(values);
                }

                return CachedValueProvider.Result.create(
                    blockNames.toArray(new String[0]),
                    FileIndexCaches.getModificationTrackerForIndexId(project, TwigBlockIndexExtension.KEY)
                );
            },
            false
        );

        for (String block : blocks) {
            processor.process(block);
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        // upstream: FileBasedIndex.AllKeysQuery + processFilesContainingAllKeys(List<AllKeysQuery>, scope, processor)
        Set<VirtualFile> virtualFiles = new LinkedHashSet<>();
        FileBasedIndex.getInstance().processFilesContainingAllKeys(
            TwigBlockIndexExtension.KEY,
            List.of("block"),
            GlobalSearchScope.allScope(project),
            value -> value.contains(name),
            virtualFiles::add
        );

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles)) {
            if (!(psiFile instanceof TwigFile file)) {
                continue;
            }

            Set<NavigationItem> items = new LinkedHashSet<>();
            for (TwigBlock twigBlock : TwigUtil.getBlocksInFile(file)) {
                items.add(NavigationItemExStateless.create(
                    twigBlock.getTarget(),
                    twigBlock.getName(),
                    Symfony2Icons.TWIG_BLOCK_OVERWRITE,
                    "Block",
                    true
                ));
            }

            for (NavigationItem item : items) {
                processor.process(item);
            }
        }
    }
}
