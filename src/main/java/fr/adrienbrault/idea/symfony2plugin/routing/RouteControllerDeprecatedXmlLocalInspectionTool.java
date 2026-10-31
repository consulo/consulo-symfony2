package fr.adrienbrault.idea.symfony2plugin.routing;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class RouteControllerDeprecatedInspection$RouteControllerDeprecatedXmlLocalInspectionTool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteControllerDeprecatedXmlLocalInspectionTool extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyXmlPsiElementVisitor(project, holder);
    }

    @Override
    public String getShortName() {
        return "RouteControllerDeprecatedInspectionXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Action in XML definition");
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
        private final Project project;
        private final ProblemsHolder holder;
        private @Nullable ElementPattern<PsiElement> routeControllerPattern = null;

        private MyXmlPsiElementVisitor(Project project, ProblemsHolder holder) {
            this.project = project;
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getRouteControllerPattern().accepts(element)) {
                inspectController(element);
            }

            super.visitElement(element);
        }

        private ElementPattern<PsiElement> getRouteControllerPattern() {
            if (routeControllerPattern == null) {
                routeControllerPattern = XmlHelper.getRouteControllerPattern();
            }

            return routeControllerPattern;
        }

        private void inspectController(PsiElement element) {
            PsiElement parent = element.getParent();
            if (parent == null) {
                return;
            }

            String text = RouteXmlReferenceContributor.getControllerText(parent);
            if (text == null) {
                return;
            }

            RouteControllerDeprecatedInspection.hasDeprecatedActionOrClass(project, element, text, holder);
        }
    }
}
