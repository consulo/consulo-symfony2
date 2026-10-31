package fr.adrienbrault.idea.symfony2plugin.routing.documentation;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigLanguage;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.DumbService;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * TODO consulo-port: platform.backend.documentation API missing (DocumentationTargetProvider, DocumentationTarget; could become
 *  LanguageDocumentationProvider.getCustomDocumentationElement + generateDoc). Was "implements DocumentationTargetProvider"
 *  registered as platform.backend.documentation.targetProvider; left unregistered, documentationTargets(PsiFile, int) dropped.
 */
public class RouteDocumentationTargetProvider {
    public @Nullable RouteDocumentationContext resolveContext(PsiFile file, int offset) {
        if (!file.isValid() || !Symfony2ProjectComponent.isEnabled(file.getProject()) || DumbService.isDumb(file.getProject())) {
            return null;
        }

        if (file instanceof PhpFile) {
            PsiElement leaf = file.findElementAt(offset);
            return leaf != null ? php(leaf) : null;
        }

        PsiFile twigFile = file.getViewProvider().getPsi(TwigLanguage.INSTANCE);
        if (twigFile == null) {
            return null;
        }

        PsiElement leaf = twigFile.findElementAt(offset);
        return leaf != null ? twig(leaf) : null;
    }

    private @Nullable RouteDocumentationContext php(PsiElement leaf) {
        StringLiteralExpression literal = PsiTreeUtil.getParentOfType(leaf, StringLiteralExpression.class, false);
        if (literal == null) {
            return null;
        }

        // #[Route('/blog', name: 'app_<caret>blog')]
        String routeName = RouteUsageUtil.getRouteNameForDeclaration(literal);
        if (routeName != null) {
            return new RouteDocumentationContext(routeName, literal);
        }

        // generateUrl('app_<caret>blog') / redirectToRoute(route: 'app_<caret>blog')
        if (MethodMatcher.getMatchedSignatureWithDepth(literal, PhpRouteReferenceContributor.GENERATOR_SIGNATURES) == null) {
            return null;
        }

        return new RouteDocumentationContext(literal.getContents(), literal);
    }

    private @Nullable RouteDocumentationContext twig(PsiElement leaf) {
        // {{ path(<caret>'app_blog') }} / {{ url('app_blog<caret>') }}
        PsiElement element = null;
        for (PsiElement candidate : RouteUsageUtil.getCandidateElements(leaf)) {
            if (TwigPattern.getAutocompletableRoutePattern().accepts(candidate)
                && TwigUtil.isValidStringWithoutInterpolatedOrConcat(candidate)) {
                element = candidate;
                break;
            }
        }

        if (element == null) {
            return null;
        }

        return new RouteDocumentationContext(element.getText(), element);
    }

    public static final class RouteDocumentationContext {
        private final String routeName;
        private final PsiElement anchor;

        public RouteDocumentationContext(String routeName, PsiElement anchor) {
            this.routeName = routeName;
            this.anchor = anchor;
        }

        public String getRouteName() {
            return routeName;
        }

        public PsiElement getAnchor() {
            return anchor;
        }

        public String component1() {
            return routeName;
        }

        public PsiElement component2() {
            return anchor;
        }

        public RouteDocumentationContext copy(String routeName, PsiElement anchor) {
            return new RouteDocumentationContext(routeName, anchor);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof RouteDocumentationContext that)) {
                return false;
            }

            return routeName.equals(that.routeName) && anchor.equals(that.anchor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeName, anchor);
        }

        @Override
        public String toString() {
            return "RouteDocumentationContext(routeName=" + routeName + ", anchor=" + anchor + ")";
        }
    }
}
