package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeProviderParameter {

    private final PsiElement psiElement;

    public MethodSignatureTypeProviderParameter(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return psiElement.getProject();
    }

}
