package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeServiceSuggestionCollector implements ServiceSuggestionCollector {

    public Collection<String> collect(PsiElement psiElement, Collection<ContainerService> serviceMap) {
        return ServiceSuggestionUtil.createSuggestions(ServiceContainerUtil.getPhpAttributeConstructorTypeHint(psiElement), serviceMap);
    }
}
