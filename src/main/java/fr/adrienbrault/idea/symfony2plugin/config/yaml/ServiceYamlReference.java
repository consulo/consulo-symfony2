package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.AbstractServiceReference;

public class ServiceYamlReference extends AbstractServiceReference {

    public ServiceYamlReference(PsiElement psiElement, String serviceId) {
        super(psiElement);

        this.serviceId = serviceId;
    }

    public ServiceYamlReference(PsiElement psiElement, TextRange range, String serviceId) {
        super(psiElement, range);

        this.serviceId = serviceId;
    }

    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
