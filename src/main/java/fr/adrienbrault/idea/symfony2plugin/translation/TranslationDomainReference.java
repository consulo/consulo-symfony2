package fr.adrienbrault.idea.symfony2plugin.translation;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainReference extends PsiReferenceBase<PsiElement> implements SymfonyCompletionReference, PsiPolyVariantReference {

    private String domainName;

    public TranslationDomainReference(StringLiteralExpression element) {
        super(element);
        this.domainName = element.getContents();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(TranslationUtil.getDomainPsiFiles(getElement().getProject(), this.domainName));
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @Override
    public Object[] getCompletionVariants() {
        return TranslationUtil.getTranslationDomainLookupElements(getElement().getProject()).toArray();
    }

}
