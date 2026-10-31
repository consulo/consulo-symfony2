package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.jetbrains.php.PhpIndex;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Use the internal index (yaml, xlf, ...) to provide translations
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexTranslatorProvider implements TranslatorProvider {
    private static final Key<CachedValue<Set<String>>> TRANSLATION_DOMAINS =
        Key.create("SYMFONY_TRANSLATION_DOMAINS");

    private static final Key<CachedValue<Map<String, Set<String>>>> TRANSLATION_KEYS_BY_DOMAIN =
        Key.create("SYMFONY_TRANSLATION_KEYS_BY_DOMAIN");

    @Override
    public boolean hasTranslationKey(Project project, String keyName, String domainName) {
        return hasIndexedTranslationKey(project, keyName, domainName);
    }

    @Override
    public boolean hasDomain(Project project, String domainName) {
        return hasIndexedDomain(project, domainName);
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(Project project) {
        return getIndexedDomains(project)
            .stream()
            .map(s -> new TranslatorProviderDict.TranslationDomain(s, true))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<PsiElement> getTranslationTargets(Project project, String translationKey, String domain) {
        Collection<PsiElement> psiFoundElements = new ArrayList<>();

        Collection<VirtualFile> files = new HashSet<>();
        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domain)), virtualFile -> {
            files.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, files)) {
            psiFoundElements.addAll(TranslationUtil.getTranslationKeyTargetInsideFile(psiFile, domain, translationKey));
        }

        return psiFoundElements;
    }

    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, String domainName) {
        Collection<VirtualFile> targets = new HashSet<>();

        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domainName)), virtualFile -> {
            targets.add(virtualFile);
            return true;
        }, PhpIndex.getInstance(project).getSearchScope());

        return targets;
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(Project project, String domainName) {
        return getIndexedTranslationKeys(project, domainName)
            .stream()
            .map(key -> new TranslatorProviderDict.TranslationKey(key, true))
            .collect(Collectors.toSet());
    }

    private static Map<String, Set<String>> getTranslationKeysByDomain(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TRANSLATION_KEYS_BY_DOMAIN,
            () -> CachedValueProvider.Result.create(
                new ConcurrentHashMap<>(),
                FileIndexCaches.getModificationTrackerForIndexId(project, TranslationStubIndex.KEY)
            ),
            false
        );
    }

    public static boolean hasIndexedTranslationKey(Project project, String keyName, String domainName) {
        return getIndexedTranslationKeys(project, domainName).contains(keyName);
    }

    public static boolean hasIndexedDomain(Project project, String domainName) {
        return getIndexedDomains(project).contains(domainName);
    }

    private static Set<String> getIndexedDomains(Project project) {
        return FileIndexCaches.getIndexKeysCache(project, TRANSLATION_DOMAINS, TranslationStubIndex.KEY);
    }

    private static Set<String> getIndexedTranslationKeys(Project project, String domainName) {
        if (!hasIndexedDomain(project, domainName)) {
            return Collections.emptySet();
        }

        return getTranslationKeysByDomain(project).computeIfAbsent(domainName, domain -> {
            FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
            GlobalSearchScope scope = GlobalSearchScope.allScope(project);

            return fileBasedIndex.getValues(TranslationStubIndex.KEY, domain, scope)
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        });
    }
}
