package fr.adrienbrault.idea.symfony2plugin.form.usages;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;

/**
 * Declared in {@code FormFieldReferencesSearchExecutor.kt} upstream.
 */
public final class FormFieldUsageReference extends PsiReferenceBase<StringLiteralExpression> {
    private final PhpNamedElement myTarget;

    public FormFieldUsageReference(StringLiteralExpression element, PhpNamedElement target) {
        super(element, element.getValueRange());
        myTarget = target;
    }

    @Override
    public PsiElement resolve() {
        return myTarget;
    }

    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    /**
     * ReferencesSearch is also used by Rename. This feature deliberately only adds usages.
     */
    @Override
    public PsiElement handleElementRename(String newElementName) {
        return getElement();
    }
}
