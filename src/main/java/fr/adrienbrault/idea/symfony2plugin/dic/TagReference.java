package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import consulo.project.DumbService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TagReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private final String tagName;

    public TagReference(PsiElement psiElement, String tagName) {
        super(psiElement);
        this.tagName = tagName;
    }

    public TagReference(StringLiteralExpression element) {
        super(element);
        tagName = element.getContents();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        if (DumbService.isDumb(getElement().getProject())) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return PsiElementResolveResult.createResults(ServiceUtil.getTaggedClasses(getElement().getProject(), this.tagName));
    }

    @Override
    public Object[] getCompletionVariants() {
        if (DumbService.isDumb(getElement().getProject())) {
            return new Object[0];
        }
        return TagNameCompletionProvider.getTagLookupElements(getElement().getProject()).toArray();
    }

}
