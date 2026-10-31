package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * \Doctrine\Common\Persistence\ObjectManager::find('REPOSITORY', $foo)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ObjectManagerFindTypeProvider implements PhpTypeProvider4 {
    private static final char OBJECT_MANAGER_FIND_TRIM_KEY = 'ƃ';

    @Override
    public char getKey() {
        return 'œ';
    }

    // Index-safe only: no PhpIndex here.
    @Override
    public @Nullable PhpType getType(PsiElement e) {
        if (!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Settings settings = Settings.getEnabledInstance(e);
        if (settings == null || !settings.featureTypeProvider) {
            return null;
        }

        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "find")) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if (StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        if (methodReference.getParameters().length >= 2) {
            String signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, OBJECT_MANAGER_FIND_TRIM_KEY);
            if (signature != null) {
                return new PhpType().add("#" + getKey() + signature);
            }
        }

        return null;
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        // get back our original call
        int endIndex = expression.lastIndexOf(OBJECT_MANAGER_FIND_TRIM_KEY);
        if (endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
        if (phpNamedElements.isEmpty()) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = phpNamedElements.iterator().next();
        if (!(phpNamedElement instanceof Method method)) {
            return Collections.emptySet();
        }

        if (
            !PhpElementsUtil.isMethodInstanceOf(method, "\\Doctrine\\Common\\Persistence\\ObjectManager", "find") &&
            !PhpElementsUtil.isMethodInstanceOf(method, "\\Doctrine\\Persistence\\ObjectManager", "find")
        ) {
            return Collections.emptySet();
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if (parameter == null) {
            return Collections.emptySet();
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if (phpClass == null) {
            return Collections.emptySet();
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, phpClass);
    }
}
