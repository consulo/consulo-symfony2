package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConstraintPropertyReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private final PhpClass constraintPhpClass;

    public ConstraintPropertyReference(StringLiteralExpression psiElement, PhpClass constraintPhpClass) {
        super(psiElement);
        this.constraintPhpClass = constraintPhpClass;
    }

    @Override
    public ResolveResult[] multiResolve(boolean b) {

        List<PsiElement> psiElements = new ArrayList<>();

        String content = ((StringLiteralExpression) getElement()).getContents();
        for(Field field: constraintPhpClass.getFields()) {
            String name = field.getName();
            if(!field.isConstant() && field.getModifier().isPublic() && content.equals(name)) {
                psiElements.add(field);
            }
        }

        return PsiElementResolveResult.createResults(psiElements);
    }

    @Override
    public Object[] getCompletionVariants() {

        List<LookupElement> lookupElements = new ArrayList<>();

        for(Field field: constraintPhpClass.getFields()) {
            if(!field.isConstant() && field.getModifier().isPublic()) {

                LookupElementBuilder lookupElement = LookupElementBuilder.create(field.getName()).withIcon(Symfony2Icons.SYMFONY);

                String defaultValue = PhpElementsUtil.getStringValue(field.getDefaultValue());
                if(defaultValue != null) {
                    lookupElement = lookupElement.withTypeText(defaultValue, true);
                }

                lookupElements.add(lookupElement);
            }
        }

        return lookupElements.toArray();
    }

}
