package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class EventDispatcherTypeProvider implements PhpTypeProvider4 {
    private static final char EVENT_DISPATCHER_TRIM_KEY = 'Ɨ';

    @Override
    public char getKey() {
        return 'Ƈ';
    }

    // Index-safe only: no PhpIndex here.
    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Settings settings = Settings.getEnabledInstance(e);
        if (settings == null || !settings.featureTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if (!"dispatch".equals(methodReference.getName())) {
            return null;
        }

        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length < 2) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if (StringUtils.isBlank(refSignature)) {
            return null;
        }

        String signature;
        PsiElement parameter = parameters[1];
        if (parameter instanceof NewExpression newExpression) {
            // dispatch('foo', new FooEvent());
            ClassReference classReference = newExpression.getClassReference();
            String fqn = classReference != null ? classReference.getFQN() : null;
            if (fqn == null || StringUtils.isBlank(fqn)) {
                return null;
            }
            signature = fqn;
        }
        else if (parameter instanceof Variable variable) {
            // $event = new FooEvent();
            // dispatch('foo', $event);
            signature = PhpElementsUtil.getFirstVariableTypeInScope(variable);
            if (signature == null) {
                return null;
            }
        }
        else {
            return null;
        }

        return new PhpType().add("#" + getKey() + refSignature + EVENT_DISPATCHER_TRIM_KEY + signature);
    }

    @Nullable
    @Override
    public PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        // get back our original call
        // since phpstorm 7.1.2 we need to validate this
        int endIndex = expression.lastIndexOf(EVENT_DISPATCHER_TRIM_KEY);
        if (endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        if (!parameter.startsWith("\\")) {
            return Collections.emptySet();
        }

        PhpClass phpClass = PhpElementsUtil.getClass(project, parameter);
        if (phpClass == null) {
            return Collections.emptySet();
        }

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
        if (phpNamedElements.isEmpty()) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElements.iterator().next();
        if (!(phpNamedElement instanceof Method method)) {
            return phpNamedElements;
        }

        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return phpNamedElements;
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if (parameter == null) {
            return phpNamedElements;
        }

        // finally search the classes
        if (!PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface")) {
            return phpNamedElements;
        }

        return Collections.singletonList(phpClass);
    }
}
