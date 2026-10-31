package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class RouteSettingDeprecatedInspection$RouteSettingDeprecatedInspectionXml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteSettingDeprecatedInspectionXml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof XmlAttributeValue xmlAttributeValue) {
                    RouteSettingDeprecatedInspection.registerAttributeRequirementProblem(holder, xmlAttributeValue, "_method");
                    RouteSettingDeprecatedInspection.registerAttributeRequirementProblem(holder, xmlAttributeValue, "_scheme");
                }
                else if (element instanceof XmlAttribute xmlAttribute) {
                    RouteSettingDeprecatedInspection.registerRoutePatternProblem(holder, xmlAttribute);
                }

                super.visitElement(element);
            }
        };
    }

    @Override
    public String getShortName() {
        return "RouteSettingDeprecatedInspectionXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Route settings deprecated in XML");
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
}
