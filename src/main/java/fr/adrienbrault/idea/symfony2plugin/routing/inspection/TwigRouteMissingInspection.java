package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.jetbrains.twig.TwigLanguage;
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
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigRouteMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigRouteMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing Route");
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
        return TwigLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static void inspectTwigRouteMissing(PsiElement element, ProblemsHolder holder) {
        String text = element.getText();
        if (StringUtils.isBlank(text)) {
            return;
        }

        String routeName = RouteHelper.unescapeRouteName(text);
        if (!RouteHelper.isExistingRouteName(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Missing Route", new RouteGuessTypoQuickFix(text));
            return;
        }

        if (RouteHelper.isRouteControllerDeprecated(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;
        private @Nullable ElementPattern<?> autocompletableRoutePattern = null;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getAutocompletableRoutePattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                inspectTwigRouteMissing(element, holder);
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getAutocompletableRoutePattern() {
            if (autocompletableRoutePattern != null) {
                return autocompletableRoutePattern;
            }

            autocompletableRoutePattern = TwigPattern.getAutocompletableRoutePattern();
            return autocompletableRoutePattern;
        }
    }
}
