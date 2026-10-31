package fr.adrienbrault.idea.symfony2plugin.util.psi;

import consulo.language.pattern.PatternCondition;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParentPathPatternCondition extends PatternCondition<PsiElement> {

    private final Class<? extends PsiElement>[] classes;

    @SafeVarargs
    public ParentPathPatternCondition(Class<? extends PsiElement>... parentClasses) {
        super("Parent path pattern");
        this.classes = parentClasses;
    }

    @Override
    public boolean accepts(PsiElement psiElement, ProcessingContext processingContext) {
        for (Class<? extends PsiElement> aClass : classes) {
            psiElement = psiElement.getParent();
            if(psiElement == null || !aClass.isInstance(psiElement)) {
                return false;
            }
        }

        return true;
    }
}
