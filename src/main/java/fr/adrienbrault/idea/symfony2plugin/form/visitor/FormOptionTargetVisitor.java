package fr.adrienbrault.idea.symfony2plugin.form.visitor;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOptionEnum;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOptionTargetVisitor implements FormOptionVisitor {
    private final String optionName;

    private final Collection<PsiElement> psiElements;

    public FormOptionTargetVisitor(String optionName, Collection<PsiElement> psiElements) {
        this.optionName = optionName;
        this.psiElements = psiElements;
    }

    @Override
    public void visit(PsiElement psiElement, String option, FormClass formClass, FormOptionEnum optionEnum) {
        if(option.equalsIgnoreCase(optionName)) {
            psiElements.add(psiElement);
        }
    }
}
