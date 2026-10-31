package fr.adrienbrault.idea.symfony2plugin.config.component;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference  extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {
    public ParameterReference(StringLiteralExpression element) {
        super(element);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return ResolveResult.EMPTY_ARRAY;
    }

    @Override
    public Object[] getCompletionVariants() {

        List<LookupElement> results = new ArrayList<>();

        for(Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getElement().getProject()).entrySet()) {
            results.add(new ParameterLookupElement(entry.getValue()));
        }

        return results.toArray();
    }
}
