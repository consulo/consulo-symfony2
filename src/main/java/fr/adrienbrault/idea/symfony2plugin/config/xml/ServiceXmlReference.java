package fr.adrienbrault.idea.symfony2plugin.config.xml;

import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.AbstractServiceReference;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceXmlReference extends AbstractServiceReference {

    public ServiceXmlReference(PsiElement element, String serviceId) {
        super(element);
        this.serviceId = serviceId;
    }


    public Object[] getVariants() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public Object[] getCompletionVariants() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
}
