package fr.adrienbrault.idea.symfony2plugin.form.visitor;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOptionEnum;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface FormOptionVisitor {
    void visit(PsiElement psiElement, String option, FormClass formClass, FormOptionEnum optionEnum);
}
