package fr.adrienbrault.idea.symfony2plugin.stubs.util;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.index.io.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexUtil {
    public static void forceReindex() {
        ID<?,?>[] indexIds = new ID<?,?>[] {
            ContainerBuilderStubIndex.KEY,
            ContainerParameterStubIndex.KEY,
            DoctrineMetadataFileStubIndex.KEY,
            EventAnnotationStubIndex.KEY,
            FileResourcesIndex.KEY,
            PhpTwigTemplateUsageStubIndex.KEY,
            RoutesStubIndex.KEY,
            ServicesDefinitionStubIndex.KEY,
            ServicesTagStubIndex.KEY,
            TwigExtendsStubIndex.KEY,
            TwigIncludeStubIndex.KEY,
            TwigMacroFunctionStubIndex.KEY,
            TranslationStubIndex.KEY,
            TwigBlockIndexExtension.KEY,
            PhpAttributeIndex.KEY
        };

        for(ID<?,?> id: indexIds) {
            FileBasedIndex.getInstance().requestRebuild(id, new Throwable());
        }
    }

    /**
     * "FileBasedIndex.getInstance().getAllKeys" does provide not only project keys, it needs a project filter.
     *
     * @link <a href="https://intellij-support.jetbrains.com/hc/en-us/community/posts/206766415/comments/207499699">...</a>
     */
    public static <K> Collection<K> getAllKeysForProject(ID<K, ?> indexId, Project project) {
        Set<K> items = new HashSet<>();

        FileBasedIndex index = FileBasedIndex.getInstance();
        for (K s : index.getAllKeys(indexId, project)) {
            boolean inScope = !index.processValues(indexId, s, null, (file, value) -> false, GlobalSearchScope.allScope(project));
            if (inScope) {
                items.add(s);
            }
        }

        return items;
    }
}
