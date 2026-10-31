package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.xml.language.XMLLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.InspectionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class ControllerMethodInspection$ControllerMethodInspectionXml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ControllerMethodInspectionXml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyXmlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ControllerMethodInspectionXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Missing Controller Action in XML");
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
        return XMLLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyXmlPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;
        private @Nullable ElementPattern<?> routeControllerPattern = null;

        private MyXmlPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getRouteControllerPattern().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                if (StringUtils.isNotBlank(text)) {
                    InspectionUtil.inspectController(element, text, holder, new ControllerMethodInspection.XmlLazyRouteName(element));
                }
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getRouteControllerPattern() {
            if (routeControllerPattern != null) {
                return routeControllerPattern;
            }

            routeControllerPattern = XmlHelper.getRouteControllerPattern();
            return routeControllerPattern;
        }
    }
}
