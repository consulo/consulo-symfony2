package fr.adrienbrault.idea.symfony2plugin.form.usages;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpTypeDeclaration;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiManager;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Top-level functions of {@code FormFieldUsageTargetUtil.kt}.
 */
public final class FormFieldUsageTargetUtilKt {
    /**
     * Keep prefixes, argument counts and getter precedence in one place.
     */
    private enum FormAccessor {
        GET("get", 0),
        IS("is", 0),
        HAS("has", 0),
        SET("set", 1);

        private final String myPrefix;
        private final int myArgumentCount;

        FormAccessor(String prefix, int argumentCount) {
            myPrefix = prefix;
            myArgumentCount = argumentCount;
        }

        public String getPrefix() {
            return myPrefix;
        }

        public int getArgumentCount() {
            return myArgumentCount;
        }
    }

    private FormFieldUsageTargetUtilKt() {
    }

    public static @Nullable PhpClass formTargetClass(PhpNamedElement target) {
        if (target instanceof Field field) {
            return field.getContainingClass();
        }
        else if (target instanceof Method method) {
            return method.getContainingClass();
        }

        return null;
    }

    public static boolean isSupportedFormTarget(PhpNamedElement target) {
        if (target instanceof Field field) {
            return isSupportedFormField(field);
        }
        else if (target instanceof Method method) {
            PhpClass phpClass = formTargetClass(method);
            return phpClass != null && !phpClass.isInterface() && !phpClass.isTrait() &&
                formAccessor(method) != null;
        }

        return false;
    }

    private static boolean isSupportedFormField(Field field) {
        if (!field.getModifier().isPublic() || field.getModifier().isStatic() || field.isConstant()) {
            return false;
        }

        PhpClass containingClass = field.getContainingClass();
        return containingClass != null && !containingClass.isInterface() && !containingClass.isTrait();
    }

    /**
     * Check public PropertyAccessor methods and their callable signatures.
     * Optional trailing arguments are allowed; getters must be able to return a value.
     */
    private static @Nullable FormAccessor formAccessor(Method method) {
        if (!method.getAccess().isPublic() || method.isStatic()) {
            return null;
        }

        String name = method.getName();
        FormAccessor accessor = null;
        for (FormAccessor entry : FormAccessor.values()) {
            String prefix = entry.getPrefix();
            if (name.regionMatches(true, 0, prefix, 0, prefix.length()) && name.length() > prefix.length()) {
                accessor = entry;
                break;
            }
        }

        if (accessor == null) {
            return null;
        }

        Parameter[] parameters = method.getParameters();
        if (parameters.length < accessor.getArgumentCount()) {
            return null;
        }

        for (int i = accessor.getArgumentCount(); i < parameters.length; i++) {
            if (!parameters[i].isOptional()) {
                return null;
            }
        }

        if (accessor == FormAccessor.SET) {
            return accessor;
        }

        PhpTypeDeclaration typeDeclaration = PsiTreeUtil.getChildOfType(method, PhpTypeDeclaration.class);
        String returnType = typeDeclaration != null ? typeDeclaration.getText() : null;
        if ("void".equals(returnType) || "never".equals(returnType)) {
            return null;
        }

        return accessor;
    }

    /**
     * Resolve on the actual data class so an override does not count as a reference to its parent.
     * Properties and setters reuse the existing form-field navigation and name normalization.
     */
    public static boolean resolvesToFormTarget(StringLiteralExpression literal, PhpClass dataClass, PhpNamedElement target) {
        PsiManager manager = PsiManager.getInstance(literal.getProject());
        FormAccessor accessor = target instanceof Method method ? formAccessor(method) : null;

        if (target instanceof Field || accessor == FormAccessor.SET) {
            for (ResolveResult resolveResult : new FormUnderscoreMethodReference(literal, dataClass).multiResolve(false)) {
                if (manager.areElementsEquivalent(resolveResult.getElement(), target)) {
                    return true;
                }
            }

            return false;
        }

        if (accessor == null) {
            return false;
        }

        Method getter = resolveFormGetter(literal, dataClass);
        if (getter == null) {
            return false;
        }

        return manager.areElementsEquivalent(getter, target);
    }

    /**
     * Read access uses the first callable getter in PropertyAccessor order.
     * Twig's shortcut helpers only check names and do not enforce these signatures.
     */
    private static @Nullable Method resolveFormGetter(StringLiteralExpression literal, PhpClass dataClass) {
        String suffix = StringUtils.camelize(literal.getContents());

        for (FormAccessor accessor : FormAccessor.values()) {
            if (accessor == FormAccessor.SET) {
                continue;
            }

            Method method = dataClass.findMethodByName(accessor.getPrefix() + suffix);
            if (method == null) {
                continue;
            }

            if (formAccessor(method) != accessor) {
                continue;
            }

            return method;
        }

        return null;
    }
}
