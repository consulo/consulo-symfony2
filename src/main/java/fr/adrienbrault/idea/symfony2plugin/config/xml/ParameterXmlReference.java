package fr.adrienbrault.idea.symfony2plugin.config.xml;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterXmlReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String parameterName;

    public ParameterXmlReference(PsiElement psiElement, String parameterName) {
        super(psiElement);
        this.parameterName = parameterName;
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Collection<PsiElement> targets = new ArrayList<>();

        targets.addAll(
            DotEnvUtil.getEnvironmentVariableTargetsForParameter(getElement().getProject(), this.parameterName)
        );

        targets.addAll(ServiceUtil.getServiceClassTargets(getElement().getProject(), this.parameterName));

        return PsiElementResolveResult.createResults(targets);
    }

    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
