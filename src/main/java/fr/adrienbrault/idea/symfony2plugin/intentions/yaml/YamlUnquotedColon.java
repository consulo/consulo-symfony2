package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlUnquotedColon extends LocalInspectionTool {
    public static final String MESSAGE = "Using a colon in the unquoted mapping value is deprecated since Symfony 2.8 and will throw a ParseException in 3.0";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();

        if (!Symfony2ProjectComponent.isEnabled(project) || !SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "YamlUnquotedColon";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Colon in the unquoted mapping");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Yaml");
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

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            // every array element implements this interface
            // check for inside "foo: <foo: foo>"
            if (!isIllegalColonExpression(element)) {
                super.visitElement(element);
                return;
            }

            // element need to be inside key value
            PsiElement yamlKeyValue = element.getParent();
            if (!(yamlKeyValue instanceof YAMLKeyValue)) {
                super.visitElement(element);
                return;
            }

            // invalid inline item "foo: foo: foo", also check text length
            String text = yamlKeyValue.getText();
            if (!text.contains(": ") || text.contains("\n") || text.length() > 200) {
                super.visitElement(element);
                return;
            }

            // attach notification "foo: <foo: foo>"
            myHolder.registerProblem(
                element,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            );

            super.visitElement(element);
        }

        private boolean isIllegalColonExpression(PsiElement element) {
            if (!(element instanceof YAMLCompoundValue)) {
                return false;
            }

            return element.getNode().getElementType() == YAMLElementTypes.MAPPING;
        }
    }
}
