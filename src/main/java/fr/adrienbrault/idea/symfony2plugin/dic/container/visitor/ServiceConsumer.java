package fr.adrienbrault.idea.symfony2plugin.dic.container.visitor;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceFileDefaults;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceConsumer {

    private final PsiElement psiElement;

    private final String serviceId;

    private final AttributeValueInterface attributeValue;

    private final ServiceFileDefaults defaults;

    public ServiceConsumer(PsiElement psiElement, String serviceId, AttributeValueInterface attributeValue, ServiceFileDefaults defaults) {
        this.psiElement = psiElement;
        this.serviceId = serviceId;
        this.attributeValue = attributeValue;
        this.defaults = defaults;
    }

    public String getServiceId() {
        return serviceId;
    }

    public AttributeValueInterface attributes() {
        return attributeValue;
    }

    public PsiElement getPsiElement() {
        return psiElement;
    }

    public ServiceFileDefaults getDefaults() {
        return defaults;
    }
}
