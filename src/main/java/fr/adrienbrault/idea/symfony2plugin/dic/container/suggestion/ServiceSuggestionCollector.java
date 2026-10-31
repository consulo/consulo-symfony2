package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceSuggestionCollector {
    Collection<String> collect(PsiElement psiElement, Collection<ContainerService> serviceMap);
}
