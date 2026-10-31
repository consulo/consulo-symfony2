package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class ContainerConstantInspection$ContainerConstantYamlInspection
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ContainerConstantYamlInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof YAMLScalar yamlScalar) {
                    ContainerConstantInspection.visitYamlElement(yamlScalar, holder);
                }

                super.visitElement(element);
            }
        };
    }

    @Override
    public String getShortName() {
        return "ContainerConstantYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Constant not found in yaml definition");
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
}
