package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpRouteMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "PhpRouteMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Route Missing");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Route");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static void inspectPhpRouteMissing(String routeName, PsiElement element, ProblemsHolder holder) {
        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(element, 0)
            .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
            .match();

        if (methodMatchParameter == null) {
            return;
        }

        if (!RouteHelper.isExistingRouteName(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Missing Route", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new RouteGuessTypoQuickFix(routeName));
            return;
        }

        if (RouteHelper.isRouteControllerDeprecated(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;
        private @Nullable ElementPattern<?> methodWithFirstStringPattern = null;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof StringLiteralExpression && getMethodWithFirstStringPattern().accepts(element)) {
                String contents = PhpElementsUtil.getStringValue(element);
                if (StringUtils.isNotBlank(contents)) {
                    inspectPhpRouteMissing(contents, element, holder);
                }
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getMethodWithFirstStringPattern() {
            if (methodWithFirstStringPattern != null) {
                return methodWithFirstStringPattern;
            }

            methodWithFirstStringPattern = PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern();
            return methodWithFirstStringPattern;
        }
    }
}
