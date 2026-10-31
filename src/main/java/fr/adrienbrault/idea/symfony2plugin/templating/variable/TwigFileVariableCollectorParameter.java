package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileVariableCollectorParameter {

    private final PsiElement psiElement;
    private final Set<VirtualFile> visitedFiles;
    private final Project project;
    private final PsiFile containingFile;

    public TwigFileVariableCollectorParameter(PsiElement psiElement, PsiFile containingFile, Set<VirtualFile> visitedFiles) {
        this.psiElement = psiElement;
        this.project = psiElement.getProject();
        this.containingFile = containingFile;
        this.visitedFiles = visitedFiles;
    }

    public PsiElement getElement() {
        return psiElement;
    }

    public Project getProject() {
        return project;
    }

    public PsiFile getContainingFile() {
        return containingFile;
    }

    public Set<VirtualFile> getVisitedFiles() {
        return visitedFiles;
    }

}
