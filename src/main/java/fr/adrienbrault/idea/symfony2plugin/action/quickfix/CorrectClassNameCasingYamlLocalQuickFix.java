package fr.adrienbrault.idea.symfony2plugin.action.quickfix;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class CorrectClassNameCasingYamlLocalQuickFix implements LocalQuickFix {

    private final String replacementFQN;

    public CorrectClassNameCasingYamlLocalQuickFix(String replacementFQN) {

        this.replacementFQN = replacementFQN;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Use " + replacementFQN);
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement psiElement1 = descriptor.getPsiElement();
        YAMLKeyValue replacement = YamlPsiElementFactory.createFromText(
                project,
                YAMLKeyValue.class,
                "class: " + replacementFQN
        );

        if (replacement != null && replacement.getValue() != null) {
            psiElement1.replace(replacement.getValue());
        }
    }
}
