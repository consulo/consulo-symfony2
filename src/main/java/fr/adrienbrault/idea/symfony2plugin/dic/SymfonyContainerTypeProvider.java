package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class SymfonyContainerTypeProvider implements PhpTypeProvider4 {
    private static final char TRIM_KEY = 'Ƃ';

    @Override
    public char getKey() {
        return 'Ő';
    }

    // Index-safe only: no PhpIndex here.
    @Override
    public @Nullable PhpType getType(PsiElement e) {
        // container calls are only on "get" methods
        if (!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Settings settings = Settings.getEnabledInstance(e);
        if (settings == null || !settings.featureTypeProvider) {
            return null;
        }

        // container calls are only on "get" methods
        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "get")) {
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
        PhpTypeProviderUtil.SignatureSplit signature = PhpTypeProviderUtil.resolveSignatureSplit(expression, TRIM_KEY, project);
        if (signature == null) {
            return Collections.emptySet();
        }

        PhpNamedElement phpNamedElement = signature.elements().iterator().next();
        if (!(phpNamedElement instanceof Method method)) {
            return signature.elements();
        }

        String parameter = PhpTypeProviderUtil.getResolvedParameter(signature.phpIndex(), signature.parameter());
        if (parameter == null) {
            return signature.elements();
        }

        // finally search the classes
        if (ServiceContainerUtil.isServiceGetMethod(method)) {
            ContainerService containerService = ContainerCollectionResolver.getService(project, parameter);
            if (containerService != null) {
                Set<PhpNamedElement> phpClasses = new LinkedHashSet<>();
                for (String className : containerService.getClassNames()) {
                    phpClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(className));
                }

                return phpClasses;
            }
        }

        return signature.elements();
    }
}
