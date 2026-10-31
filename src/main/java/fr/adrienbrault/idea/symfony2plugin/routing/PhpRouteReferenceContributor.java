package fr.adrienbrault.idea.symfony2plugin.routing;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerReference;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpRouteReferenceContributor extends PsiReferenceContributor {

    public static final MethodMatcher.CallToSignature[] GENERATOR_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "generateUrl"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "redirectToRoute"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "generateUrl"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "redirectToRoute"),

        // Symfony 4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "generateUrl"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "redirectToRoute"),

        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface", "generate"),

    };

    private static final MethodMatcher.CallToSignature[] FORWARD_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "forward"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "forward"),

        // Symfony 4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "forward"),
    };

    private static final MethodMatcher.CallToSignature[] ROUTE_CONTROLLER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait", "controller"),
    };

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {

                    MethodMatcher.MethodMatchParameter matchedSignatureWithDepth = MethodMatcher.getMatchedSignatureWithDepth(psiElement, GENERATOR_SIGNATURES);
                    if (matchedSignatureWithDepth == null) {
                        return new PsiReference[0];
                    }

                    // @TODO: find better solution; prevent duplicate on proxy frpm twig to php eg on path() or url()
                    MethodReference methodReference = matchedSignatureWithDepth.getMethodReference();
                    PhpExpression classReference = methodReference.getClassReference();
                    if (classReference != null && "____proxy____".equalsIgnoreCase(classReference.getName())) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new RouteReference((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, FORWARD_SIGNATURES) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ControllerReference((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, ROUTE_CONTROLLER_SIGNATURES) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ControllerReference((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
