package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * <service class="Foo">
 *   <call method="setFoo">
 *     <argument type="service" id="<caret>" />
 *   </call>
 * </service>
 */
public class XmlCallServiceSuggestionCollector implements ServiceSuggestionCollector {

    public Collection<String> collect(PsiElement psiElement, Collection<ContainerService> serviceMap) {
        if(!(psiElement.getContainingFile() instanceof XmlFile) || psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return Collections.emptyList();
        }

        return ServiceSuggestionUtil.createSuggestions(ServiceContainerUtil.getXmlCallTypeHint(
            psiElement, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
        ), serviceMap);
    }
}
