package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
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
 * Inspects route names used in Twig comparison expressions against app.request.attributes.get('_route'):
 *
 *   app.request.attributes.get('_route') == 'my_route'
 *   app.request.attributes.get('_route') != 'my_route'
 *   app.request.attributes.get('_route') is same as('my_route')
 *   app.request.attributes.get('_route') in ['route_a', 'route_b']
 *   app.request.attributes.get('_route') not in ['route_a', 'route_b']
 *
 * Note: 'starts with' is intentionally excluded because the string is a prefix, not a full route name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigRouteCompareInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if ((TwigPattern.getTwigRouteComparePattern().accepts(element)
                        || TwigPattern.getTwigRouteSameAsPattern().accepts(element)
                        || TwigPattern.getTwigRouteInArrayPattern().accepts(element))
                    && TwigPattern.isRouteCompareContext(element)
                    && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)
                ) {
                    inspectTwigRouteCompare(element, holder);
                }

                super.visitElement(element);
            }
        };
    }

    @Override
    public String getShortName() {
        return "TwigRouteCompareInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing Route in comparison");
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

    private static void inspectTwigRouteCompare(PsiElement element, ProblemsHolder holder) {
        String routeName = element.getText();
        if (StringUtils.isBlank(routeName)) {
            return;
        }

        if (!RouteHelper.isExistingRouteName(element.getProject(), routeName)) {
            holder.registerProblem(element, "Symfony: Missing Route", new RouteGuessTypoQuickFix(routeName));
        }
    }
}
