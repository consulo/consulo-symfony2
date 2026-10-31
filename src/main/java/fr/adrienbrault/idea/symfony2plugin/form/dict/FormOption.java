package fr.adrienbrault.idea.symfony2plugin.form.dict;

import consulo.language.psi.PsiElement;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOption {
    private final String option;

    private final FormClass formClass;

    private final Collection<PsiElement> psiElements = new HashSet<>();

    private final Collection<FormOptionEnum> optionEnum = new HashSet<>();

    public FormOption(String option, FormClass formClass, FormOptionEnum optionEnum, PsiElement psiElement) {
        this.option = option;
        this.formClass = formClass;

        this.psiElements.add(psiElement);
        this.optionEnum.add(optionEnum);
    }

    public String getOption() {
        return option;
    }

    public FormClass getFormClass() {
        return formClass;
    }

    public Collection<FormOptionEnum> getOptionEnum() {
        return optionEnum;
    }

    public void addOptionEnum(FormOptionEnum optionEnum) {
        this.optionEnum.add(optionEnum);
    }

    public void addTarget(PsiElement psiElement) {
        this.psiElements.add(psiElement);
    }

    public Collection<PsiElement> getPsiTargets() {
        return psiElements;
    }
}
