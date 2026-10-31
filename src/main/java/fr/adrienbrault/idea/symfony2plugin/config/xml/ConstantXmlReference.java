package fr.adrienbrault.idea.symfony2plugin.config.xml;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ConstantXmlReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String value;

    ConstantXmlReference(PsiElement element, String value) {
        super(element);
        this.value = value;
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(
            ServiceContainerUtil.getTargetsForConstant(getElement().getProject(), value)
        );
    }

    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}