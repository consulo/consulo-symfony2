package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extract translations out of compiled php file inside Symfony cache folder
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledTranslatorProvider implements TranslatorProvider {
    @Override
    public boolean hasTranslationKey(Project project, String keyName, String domainName) {
        Set<String> domainMap = TranslationIndex.getTranslationMap(project).getDomainMap(domainName);
        return domainMap != null && domainMap.contains(keyName);
    }

    @Override
    public boolean hasDomain(Project project, String domainName) {
        return TranslationIndex.getTranslationMap(project).getDomainList().contains(domainName);
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(Project project) {
        return TranslationIndex.getTranslationMap(project)
            .getDomainList()
            .stream()
            .map(TranslatorProviderDict.TranslationDomain::new)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<PsiElement> getTranslationTargets(Project project, String translationKey, String domain) {
        // targeting cache folder is not helpful for user
        return Collections.emptyList();
    }

    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, String domainName) {
        // targeting cache folder is not helpful for user
        return Collections.emptyList();
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(Project project, String domainName) {
        Set<String> domainMap = TranslationIndex.getTranslationMap(project).getDomainMap(domainName);
        if (domainMap == null) {
            return Collections.emptyList();
        }

        return domainMap.stream().map(TranslatorProviderDict.TranslationKey::new).collect(Collectors.toList());
    }
}
