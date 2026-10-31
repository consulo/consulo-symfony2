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
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class RouteControllerDeprecatedInspection$RouteControllerDeprecatedYamlLocalInspectionTool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteControllerDeprecatedYamlLocalInspectionTool extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyYamlPsiElementVisitor(project, holder);
    }

    @Override
    public String getShortName() {
        return "RouteControllerDeprecatedInspectionYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Action in yaml definition");
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
        return YAMLLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyYamlPsiElementVisitor extends PsiElementVisitor {
        private final Project project;
        private final ProblemsHolder holder;
        private @Nullable ElementPattern<PsiElement> controllerScalarPattern = null;

        private MyYamlPsiElementVisitor(Project project, ProblemsHolder holder) {
            this.project = project;
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getControllerScalarPattern().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                if (!text.isBlank()) {
                    RouteControllerDeprecatedInspection.hasDeprecatedActionOrClass(project, element, text, holder);
                }
            }

            super.visitElement(element);
        }

        private ElementPattern<PsiElement> getControllerScalarPattern() {
            if (controllerScalarPattern == null) {
                controllerScalarPattern = YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller");
            }

            return controllerScalarPattern;
        }
    }
}
