package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import consulo.language.psi.PsiElement;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceDefinitionLocatorParameter {

    private final Project project;

    private final Collection<PsiElement> psiElements;

    public ServiceDefinitionLocatorParameter(Project project, Collection<PsiElement> psiElements) {
        this.project = project;
        this.psiElements = psiElements;
    }

    public void addTarget(PsiElement psiElement) {
        this.psiElements.add(psiElement);
    }

    public void addTargets(Collection<PsiElement> psiElements) {
        this.psiElements.addAll(psiElements);
    }

    public Project getProject() {
        return project;
    }
}
