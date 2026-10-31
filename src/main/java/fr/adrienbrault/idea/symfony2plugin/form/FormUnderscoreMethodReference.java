package fr.adrienbrault.idea.symfony2plugin.form;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUnderscoreMethodReference extends PsiPolyVariantReferenceBase<StringLiteralExpression> implements SymfonyCompletionReference {
    private final PhpClass phpClass;

    public FormUnderscoreMethodReference(StringLiteralExpression element, PhpClass phpClass) {
        super(element);
        this.phpClass = phpClass;
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String contents = getElement().getContents();
        if(org.apache.commons.lang3.StringUtils.isBlank(contents)) {
            return new ResolveResult[0];
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        Set<String> methods = getCamelizeAndUnderscoreString(contents);

        // provide setter fallback for non model class or or unknown methods
        for (String value : methods) {
            Method method = phpClass.findMethodByName("set" + value);
            if (method != null) {
                psiElements.add(method);
            }
        }

        // property path
        psiElements.addAll(this.phpClass.getFields().stream()
            .filter(field -> !field.isConstant() && field.getModifier().isPublic() && methods.contains(field.getName()))
            .toList()
        );

        return PsiElementResolveResult.createResults(psiElements);
    }

    @Override
    public Object[] getCompletionVariants() {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        visitPropertyPath(
            this.phpClass,
            pair -> lookupElements.add(new PhpFormPropertyMethodLookupElement(pair.getSecond(), pair.getFirst()))
        );

        return lookupElements.toArray();
    }

    public static void visitPropertyPath(PhpClass phpClass, Consumer<Pair<String, PhpNamedElement>> consumer) {
        // provide setter fallback for non-model class or unknown methods
        for (Method method: phpClass.getMethods()) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")) {
                consumer.accept(new Pair<>(StringUtils.lcfirst(name.substring(3)), method));
            }
        }

        // Symfony\Component\PropertyAccess\PropertyAccessor::getWriteAccessInfo
        // property: public $foobar
        for (Field field: phpClass.getFields()) {
            if (!field.isConstant() && field.getModifier().isPublic()) {
                consumer.accept(new Pair<>(field.getName(), field));
            }
        }
    }

    private Set<String> getCamelizeAndUnderscoreString(String string) {
        TreeSet<String> strings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        string = StringUtils.lcfirst(string);

        strings.addAll(Arrays.asList(
            StringUtils.underscore(string),
            StringUtils.camelize(string),
            string
        ));

        return strings;
    }
}
