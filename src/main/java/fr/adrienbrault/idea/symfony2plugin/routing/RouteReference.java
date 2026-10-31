package fr.adrienbrault.idea.symfony2plugin.routing;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class RouteReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private final String routeName;

    public RouteReference(StringLiteralExpression element) {
        super(element);
        this.routeName = element.getContents();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(RouteHelper.getRouteDefinitionTargets(getElement().getProject(), this.routeName));
    }

    @Override
    public Object[] getCompletionVariants() {
        return RouteHelper.getRoutesLookupElements(getElement().getProject()).toArray();
    }

}
