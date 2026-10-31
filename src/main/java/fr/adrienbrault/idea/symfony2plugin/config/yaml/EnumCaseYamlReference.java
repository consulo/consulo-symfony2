package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.language.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Collection;

/**
 * key: !php/enum Foo\Bar::BAZ
 * key: !php/enum Foo\Bar::BAZ->value
 */
public class EnumCaseYamlReference extends PsiPolyVariantReferenceBase<YAMLScalar> {

    private static final String SCOPE_OPERATOR = "::";
    private static final String VALUE_ACCESSOR = "->";

    public EnumCaseYamlReference(YAMLScalar element) {
        super(element);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String value = getElement().getTextValue();
        int scopeOperatorPos = value.indexOf(SCOPE_OPERATOR);
        if (scopeOperatorPos < 1) {
            return ResolveResult.EMPTY_ARRAY;
        }

        String className = value.substring(0, scopeOperatorPos);
        String caseName = getCaseName(value);

        Collection<PsiElement> targets = new ArrayList<>();
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(getElement().getProject(), className)) {
            if (phpClass.isEnum()) {
                phpClass.getEnumCases().stream()
                    .filter(enumCase -> caseName.equals(enumCase.getName()))
                    .forEach(targets::add);
            }
        }

        return PsiElementResolveResult.createResults(targets);
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        String value = getValue();
        int scopeOperatorPos = value.indexOf(SCOPE_OPERATOR);
        if (scopeOperatorPos > 0) {
            int valueAccessorPos = value.indexOf(VALUE_ACCESSOR, scopeOperatorPos);
            newElementName = value.substring(0, scopeOperatorPos + SCOPE_OPERATOR.length())
                + newElementName
                + (valueAccessorPos > -1 ? value.substring(valueAccessorPos) : "");
        }

        return super.handleElementRename(newElementName);
    }

    private static String getCaseName(String value) {
        String caseName = value.substring(value.indexOf(SCOPE_OPERATOR) + SCOPE_OPERATOR.length());
        int valueAccessorPos = caseName.indexOf(VALUE_ACCESSOR);

        return valueAccessorPos > -1 ? caseName.substring(0, valueAccessorPos) : caseName;
    }
}
