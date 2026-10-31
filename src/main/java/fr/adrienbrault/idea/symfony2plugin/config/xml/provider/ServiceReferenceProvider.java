package fr.adrienbrault.idea.symfony2plugin.config.xml.provider;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.ServiceXmlReference;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new PsiReference[0];
        }

        // get the service name "service_container"
        String text = PsiElementUtils.trimQuote(psiElement.getText());

        return new PsiReference[]{ new ServiceXmlReference(psiElement, text) };
    }

    public boolean acceptsTarget(PsiElement target) {
        return Symfony2ProjectComponent.isEnabled(target);
    }
}
