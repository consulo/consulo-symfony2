package fr.adrienbrault.idea.symfony2plugin.translation;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private String domainName;
    private final StringLiteralExpression element;

    public TranslationReference(StringLiteralExpression element, String domain) {
        super(element);
        this.domainName = domain;
        this.element = element;
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<>();
        PsiElement[] psiElements = TranslationUtil.getTranslationPsiElements(this.element.getProject(), this.element.getContents(), this.domainName);

        for (PsiElement psiElement : psiElements) {
            results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[0]);
    }

    @Override
    public Object[] getCompletionVariants() {
        return TranslationUtil.getTranslationLookupElementsOnDomain(getElement().getProject(), domainName).toArray();
    }

}
