package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Extract domain information based on the the compiled debug container inisde the cache folder
 *
 * * var/cache/...container.xml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledContainerTranslatorProvider implements TranslatorProvider {
    @Override
    public boolean hasTranslationKey(Project project, String keyName, String domainName) {
        // we only have the file; content should already be inside our much faster index
        return false;
    }

    @Override
    public boolean hasDomain(Project project, String domainName) {
        return TranslationUtil.hasDomainInsideCompiledContainer(project, domainName);
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(Project project) {
        return TranslationUtil.getDomainsFromContainer(project).stream()
            .map(TranslatorProviderDict.TranslationDomain::new)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<PsiElement> getTranslationTargets(Project project, String translationKey, String domain) {
        return TranslationUtil.getTranslationKeyFromCompiledContainerDomain(project, domain, translationKey);
    }

    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, String domainName) {
        return TranslationUtil.getDomainFilesFromCompiledContainer(project, domainName);
    }

    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(Project project, String domainName) {
        // we only have the file; content should already be inside our much faster index
        return Collections.emptyList();
    }
}
