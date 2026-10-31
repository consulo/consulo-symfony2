package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.jetbrains.php.lang.psi.elements.PhpClass;
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
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.quickfix.CorrectClassNameCasingYamlLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * Check if class exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlClassInspection extends LocalInspectionTool {
    public static final String MESSAGE_WRONG_CASING = "Wrong class casing";
    public static final String MESSAGE_MISSING_CLASS = "Missing class";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "YamlClassInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Yaml: Missing Class");
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

    private static void inspectYamlClass(PsiElement psiElement, ProblemsHolder holder) {
        String className = PsiElementUtils.getText(psiElement);

        Project project = holder.getProject();

        if (YamlHelper.isValidParameterName(className)) {
            String resolvedParameter = ContainerCollectionResolver.resolveParameter(project, className);
            if (resolvedParameter != null && PhpElementsUtil.hasClassOrInterface(project, resolvedParameter)) {
                return;
            }
        }

        PhpClass foundClass = PhpElementsUtil.getClassInterface(project, className);
        if (foundClass == null) {
            holder.registerProblem(psiElement, YamlClassInspection.MESSAGE_MISSING_CLASS, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
        else if (!foundClass.getPresentableFQN().equals(className)) {
            holder.registerProblem(psiElement, YamlClassInspection.MESSAGE_WRONG_CASING, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CorrectClassNameCasingYamlLocalQuickFix(foundClass.getPresentableFQN()));
        }
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private @Nullable ElementPattern<?> mySingleLineClassPattern;
        private @Nullable ElementPattern<?> myParameterClassPattern;
        private @Nullable ElementPattern<?> myInsideServiceKeyPattern;
        private @Nullable ElementPattern<?> myServiceIdKeyValuePattern;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if ((getSingleLineClassPattern().accepts(psiElement) || getParameterClassPattern().accepts(psiElement)) && getInsideServiceKeyPattern().accepts(psiElement)) {
                // foobar.foo:
                //   class: Foobar\Foo
                inspectYamlClass(psiElement, myHolder);
            }
            else if (psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && getServiceIdKeyValuePattern().accepts(psiElement.getParent())) {
                // Foobar\Foo: ~
                String text = PsiElementUtils.getText(psiElement);
                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                    PsiElement yamlKeyValue = psiElement.getParent();
                    if (yamlKeyValue instanceof YAMLKeyValue yamlKeyValueElement && YamlHelper.getYamlKeyValue(yamlKeyValueElement, "resource") == null && YamlHelper.getYamlKeyValue(yamlKeyValueElement, "exclude") == null) {
                        inspectYamlClass(psiElement, myHolder);
                    }
                }
            }

            super.visitElement(psiElement);
        }

        private ElementPattern<?> getSingleLineClassPattern() {
            ElementPattern<?> pattern = mySingleLineClassPattern != null ? mySingleLineClassPattern : YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class");
            mySingleLineClassPattern = pattern;
            return pattern;
        }

        private ElementPattern<?> getParameterClassPattern() {
            ElementPattern<?> pattern = myParameterClassPattern != null ? myParameterClassPattern : YamlElementPatternHelper.getParameterClassPattern();
            myParameterClassPattern = pattern;
            return pattern;
        }

        private ElementPattern<?> getInsideServiceKeyPattern() {
            ElementPattern<?> pattern = myInsideServiceKeyPattern != null ? myInsideServiceKeyPattern : YamlElementPatternHelper.getInsideServiceKeyPattern();
            myInsideServiceKeyPattern = pattern;
            return pattern;
        }

        private ElementPattern<?> getServiceIdKeyValuePattern() {
            ElementPattern<?> pattern = myServiceIdKeyValuePattern != null ? myServiceIdKeyValuePattern : YamlElementPatternHelper.getServiceIdKeyValuePattern();
            myServiceIdKeyValuePattern = pattern;
            return pattern;
        }
    }
}
