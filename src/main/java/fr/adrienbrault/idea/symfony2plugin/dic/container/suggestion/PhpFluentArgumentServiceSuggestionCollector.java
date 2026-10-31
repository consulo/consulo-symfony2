package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;

import java.util.Collection;

/**
 * Suggests services based on the constructor argument type-hint for PHP fluent-style Symfony service configuration.
 *
 * <pre>
 * $services->set(MyService::class)
 *     ->args([
 *         service('<caret>'),
 *         ref('<caret>'),
 *     ]);
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpFluentArgumentServiceSuggestionCollector implements ServiceSuggestionCollector {

    @Override
    public Collection<String> collect(PsiElement psiElement, Collection<ContainerService> serviceMap) {
        return ServiceSuggestionUtil.createSuggestions(
            ServiceContainerUtil.getPhpFluentConstructorTypeHint(
                psiElement,
                new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
            ),
            serviceMap
        );
    }
}
