package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 */
public abstract class GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
    private final PsiElement element;
    private final Project project;

    public GotoCompletionProvider(PsiElement element) {
        this.element = element;
        this.project = element.getProject();
    }

    protected Project getProject() {
        return this.project;
    }

    protected PsiElement getElement() {
        return this.element;
    }

    @Override
    public Collection<LookupElement> getLookupElements() {
        return Collections.emptyList();
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        return Collections.emptyList();
    }
}
