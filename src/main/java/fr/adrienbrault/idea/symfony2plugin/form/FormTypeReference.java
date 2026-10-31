package fr.adrienbrault.idea.symfony2plugin.form;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReference extends PsiReferenceBase<PsiElement> implements SymfonyCompletionReference, PsiReference {

    private final StringLiteralExpression element;

    public FormTypeReference(StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return FormUtil.getFormTypeToClass(getElement().getProject(), element.getContents());
    }

    @Override
    public Object[] getCompletionVariants() {
        return FormUtil.getFormTypeLookupElements(getElement().getProject()).toArray();
    }

}
