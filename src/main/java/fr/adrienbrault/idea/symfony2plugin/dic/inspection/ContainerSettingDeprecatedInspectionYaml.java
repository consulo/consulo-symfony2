package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class ContainerSettingDeprecatedInspection$ContainerSettingDeprecatedInspectionYaml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ContainerSettingDeprecatedInspectionYaml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyYamlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ContainerSettingDeprecatedInspectionYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Container settings deprecated in Yaml");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Service");
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
        private final ProblemsHolder myHolder;
        private @Nullable ElementPattern<PsiElement> myInsideServiceKeyPattern;

        MyYamlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof YAMLKeyValue yamlKeyValue) {
                ContainerSettingDeprecatedInspection.registerYmlRoutePatternProblem(myHolder, yamlKeyValue, getInsideServiceKeyPattern());
            }

            super.visitElement(element);
        }

        private ElementPattern<PsiElement> getInsideServiceKeyPattern() {
            ElementPattern<PsiElement> pattern = myInsideServiceKeyPattern != null ? myInsideServiceKeyPattern : YamlElementPatternHelper.getInsideServiceKeyPattern();
            myInsideServiceKeyPattern = pattern;
            return pattern;
        }
    }
}
