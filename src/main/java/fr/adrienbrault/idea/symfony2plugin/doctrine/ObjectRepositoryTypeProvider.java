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
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ObjectRepositoryTypeProvider implements PhpTypeProvider4 {
    public static final char TRIM_KEY = 'ƅ';

    private static final MethodMatcher.CallToSignature[] GET_REPOSITORIES_SIGNATURES = new MethodMatcher.CallToSignature[]{
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectManager", "getRepository"),
    };

    @Override
    public char getKey() {
        return 'ő';
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

        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "getRepository")) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if (StringUtil.isEmpty(refSignature)) {
            return null;
        }

        String signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(methodReference, TRIM_KEY);
        return signature != null ? new PhpType().add("#" + getKey() + signature) : null;
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        // get back our original call
        int endIndex = expression.lastIndexOf(TRIM_KEY);
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
            return phpNamedElements;
        }

        if (!PhpElementsUtil.isMethodInstanceOf(method, GET_REPOSITORIES_SIGNATURES)) {
            return phpNamedElements;
        }

        // we can also pipe php references signatures and resolve them here
        // overwrite parameter to get string value
        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if (parameter == null) {
            return phpNamedElements;
        }

        PhpClass phpClass = EntityHelper.getEntityRepositoryClass(project, parameter);
        if (phpClass == null) {
            // self add :)
            return phpNamedElements;
        }

        return PhpTypeProviderUtil.mergeSignatureResults(phpNamedElements, phpClass);
    }
}
