package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.MethodSignatureTypeProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class MethodSignatureTypeProvider implements PhpTypeProvider4 {
    private static final char METHOD_SIGNATURE_TRIM_KEY = '\u0181';
    private static final ExtensionPointName<MethodSignatureTypeProviderExtension> METHOD_SIGNATURE_EXTENSIONS =
        ExtensionPointName.create(MethodSignatureTypeProviderExtension.class);

    @Override
    public char getKey() {
        return '\u0160';
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

        Collection<MethodSignatureSetting> signatures = getSignatureSettings(e);
        if (signatures.isEmpty()) {
            return null;
        }

        Collection<MethodSignatureSetting> matchedSignatures = getSignatureSetting(methodReference.getName(), signatures);
        if (matchedSignatures.isEmpty()) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if (StringUtil.isEmpty(refSignature)) {
            return null;
        }

        // we need the param key on getBySignature(), since we are already in the resolved method there attach it to signature
        // param can have dotted values split with \
        PsiElement[] parameters = methodReference.getParameters();
        for (MethodSignatureSetting methodSignature : matchedSignatures) {
            if (parameters.length - 1 >= methodSignature.getIndexParameter()) {
                PsiElement parameter = parameters[methodSignature.getIndexParameter()];
                if (parameter instanceof StringLiteralExpression stringLiteralExpression) {
                    String param = stringLiteralExpression.getContents();
                    if (StringUtil.isNotEmpty(param)) {
                        return new PhpType().add("#" + getKey() + refSignature + METHOD_SIGNATURE_TRIM_KEY + param);
                    }
                }

                String parameterSignature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(
                    methodReference,
                    METHOD_SIGNATURE_TRIM_KEY
                );
                if (parameterSignature != null) {
                    return new PhpType().add("#" + getKey() + parameterSignature);
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        return null;
    }

    private Collection<MethodSignatureSetting> getSignatureSettings(PsiElement psiElement) {
        List<MethodSignatureSetting> signatures = new ArrayList<>();

        // get user defined settings
        Settings settings = Settings.getInstance(psiElement);
        if (settings.objectSignatureTypeProvider) {
            List<MethodSignatureSetting> methodSignatureSettings = settings.methodSignatureSettings;
            if (methodSignatureSettings != null) {
                signatures.addAll(methodSignatureSettings);
            }
        }

        // load extension
        MethodSignatureTypeProviderExtension[] extensions = METHOD_SIGNATURE_EXTENSIONS.getExtensions();
        if (extensions.length > 0) {
            MethodSignatureTypeProviderParameter parameter = new MethodSignatureTypeProviderParameter(psiElement);
            for (MethodSignatureTypeProviderExtension extension : extensions) {
                signatures.addAll(extension.getSignatures(parameter));
            }
        }

        return signatures;
    }

    private Collection<MethodSignatureSetting> getSignatureSetting(
        @Nullable String methodName,
        Collection<MethodSignatureSetting> methodSignatureSettings
    ) {
        return methodSignatureSettings.stream()
            .filter(it -> Objects.equals(it.getMethodName(), methodName))
            .toList();
    }

    @Override
    public @Nullable Collection<PhpNamedElement> getBySignature(
        String expression,
        Set<String> visited,
        int depth,
        Project project
    ) {
        // get back our original call
        int endIndex = expression.lastIndexOf(METHOD_SIGNATURE_TRIM_KEY);
        if (endIndex == -1) {
            return null;
        }

        String originalSignature = expression.substring(0, endIndex);
        String unresolvedParameter = expression.substring(endIndex + 1);

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElements = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
        if (phpNamedElements.isEmpty()) {
            return null;
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElements.iterator().next();
        if (!(phpNamedElement instanceof Method method)) {
            return null;
        }

        Collection<MethodSignatureSetting> signatures = getSignatureSettings(method);
        if (signatures.isEmpty()) {
            return null;
        }

        String parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, unresolvedParameter);
        if (parameter == null) {
            return null;
        }

        List<PhpNamedElement> resolvedElements = new ArrayList<>();

        for (MethodSignatureSetting matchedSignature : signatures) {
            for (PhpTypeSignatureInterface signatureTypeProvider : PhpTypeSignatureTypes.DEFAULT_PROVIDER) {
                if (
                    signatureTypeProvider.getName().equals(matchedSignature.getReferenceProviderName()) &&
                    PhpElementsUtil.isMethodInstanceOf(
                        method,
                        matchedSignature.getCallTo(),
                        matchedSignature.getMethodName()
                    )
                ) {
                    Collection<? extends PhpNamedElement> byParameter = signatureTypeProvider.getByParameter(project, parameter);
                    if (byParameter != null) {
                        resolvedElements.addAll(byParameter);
                    }
                }
            }
        }

        // not good but we need return any previous types: null clears all types
        return new ArrayList<>(resolvedElements);
    }
}
