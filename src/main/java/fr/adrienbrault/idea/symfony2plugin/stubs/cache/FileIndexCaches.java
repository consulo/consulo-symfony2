package fr.adrienbrault.idea.symfony2plugin.stubs.cache;

import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.stub.StubIndexKey;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.component.util.ModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.index.io.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All FileBasedIndex are slow and cross project data, we need them every often
 * Cache values as long nothing globally change in our project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileIndexCaches {

    /**
     * @param dataHolderKey Main data to cache
     * @param dataHolderNames Cache extracted name Set
     */
    static public synchronized <T> Map<String, List<T>> getSetDataCache(final Project project, Key<CachedValue<Map<String, List<T>>>> dataHolderKey, final Key<CachedValue<Set<String>>> dataHolderNames, final ID<String, T> ID, final GlobalSearchScope scope) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> {
                Map<String, List<T>> items = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

                getIndexKeysCache(project, dataHolderNames, ID).forEach(service ->
                    items.put(service, fileBasedIndex.getValues(ID, service, scope))
                );

                return CachedValueProvider.Result.create(items, getModificationTrackerForIndexId(project, ID));
            },
            false
        );
    }

    /**
     * @param dataHolderKey Main data to cache
     * @param dataHolderNames Cache extracted name Set
     */
    static public synchronized Map<String, List<String>> getStringDataCache(final Project project, Key<CachedValue<Map<String, List<String>>>> dataHolderKey, final Key<CachedValue<Set<String>>> dataHolderNames, final ID<String, String> ID, final GlobalSearchScope scope) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> {
                Map<String, List<String>> strings = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
                getIndexKeysCache(project, dataHolderNames, ID).forEach(parameterName -> {
                    // just for secure
                    if(parameterName == null) {
                        return;
                    }

                    strings.put(parameterName, fileBasedIndex.getValues(ID, parameterName, scope));
                });

                return CachedValueProvider.Result.create(strings, getModificationTrackerForIndexId(project, ID));
            },
            false
        );
    }

    /**
     * There several methods that just need to check for names, as they also needed for value extraction, so cache them also
     */
    static public synchronized Set<String> getIndexKeysCache(final Project project, Key<CachedValue<Set<String>>> dataHolderKey, final ID<String, ?> id) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> CachedValueProvider.Result.create(SymfonyProcessors.createResult(project, id), getModificationTrackerForIndexId(project, id)),
            false
        );
    }

    public static ModificationTracker getModificationTrackerForIndexId(Project project, final ID<?, ?> id) {
        if (id instanceof StubIndexKey) {
            // stub indexes have no stamp in FileBasedIndex
            return PsiModificationTracker.getInstance(project);
        }
        return () -> FileBasedIndex.getInstance().getIndexModificationStamp(id, project);
    }
}
