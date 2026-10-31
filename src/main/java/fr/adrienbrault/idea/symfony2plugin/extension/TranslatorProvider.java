package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;

import java.util.Collection;

/**
 * Provide extension implementation for new translator source
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TranslatorProvider {
    boolean hasTranslationKey(Project project, String keyName, String domainName);
    boolean hasDomain(Project project, String domainName);

    Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(Project project);

    Collection<PsiElement> getTranslationTargets(Project project, String translationKey, String domain);

    Collection<VirtualFile> getDomainPsiFiles(final Project project, String domainName);

    Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(Project project, String domainName);
}
