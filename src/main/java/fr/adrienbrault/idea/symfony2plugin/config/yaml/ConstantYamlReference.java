package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.language.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLScalar;

public class ConstantYamlReference extends PsiPolyVariantReferenceBase<YAMLScalar> {

    public ConstantYamlReference(YAMLScalar element) {
        super(element);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        var constantName = getElement().getTextValue();
        if (StringUtils.isBlank(constantName)) {
            return ResolveResult.EMPTY_ARRAY;
        }

        return PsiElementResolveResult.createResults(
            ServiceContainerUtil.getTargetsForConstant(getElement().getProject(), constantName)
        );
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        var constantName = getValue();
        if (isClassConst(constantName)) {
            newElementName = getClassName(constantName) + "::" + newElementName;
        }

        return super.handleElementRename(newElementName);
    }

    private boolean isClassConst(String value) {
        return value.contains("::");
    }

    private String getClassName(String value) {
        return value.substring(0, value.indexOf("::"));
    }
}
