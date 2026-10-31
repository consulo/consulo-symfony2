package fr.adrienbrault.idea.symfony2plugin.form.visitor;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOptionEnum;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOptionLookupVisitor implements FormOptionVisitor {

    private final Collection<LookupElement> lookupElements;

    public FormOptionLookupVisitor(Collection<LookupElement> lookupElements) {
        this.lookupElements = lookupElements;
    }

    @Override
    public void visit(PsiElement psiElement, String option, FormClass formClass, FormOptionEnum optionEnum) {

        String typeText = formClass.getPhpClass().getPresentableFQN();
        if(typeText.lastIndexOf("\\") != -1) {
            typeText = typeText.substring(typeText.lastIndexOf("\\") + 1);
        }

        if(typeText.endsWith("Type")) {
            typeText = typeText.substring(0, typeText.length() - 4);
        }

        lookupElements.add(LookupElementBuilder.create(option)
                .withTypeText(typeText, true)
                .withIcon(Symfony2Icons.FORM_OPTION)
        );

    }

}
