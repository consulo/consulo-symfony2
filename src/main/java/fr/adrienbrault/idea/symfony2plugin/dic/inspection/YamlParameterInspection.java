package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

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
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.YAMLLanguage;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Check if service parameter exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlParameterInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new ParameterVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "YamlParameterInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Missing Parameter");
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

    private static class ParameterVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;

        private @Nullable ElementPattern<PsiElement> myServiceParameterPattern;
        private @Nullable ElementPattern<PsiElement> myInsideServiceKeyPattern;

        ParameterVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (getServiceParameterPattern().accepts(psiElement) && getInsideServiceKeyPattern().accepts(psiElement)) {
                ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                    ? myLazyServiceCollector
                    : new ContainerCollectionResolver.LazyServiceCollector(myHolder.getProject());
                myLazyServiceCollector = collector;

                invoke(psiElement, myHolder, collector);
            }

            super.visitElement(psiElement);
        }

        private void invoke(PsiElement psiElement, ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            // at least %a%
            // and not this one: %kernel.root_dir%/../web/
            // %kernel.root_dir%/../web/%webpath_modelmasks%
            String parameterName = PsiElementUtils.getText(psiElement);
            if (!YamlHelper.isValidParameterName(parameterName)) {
                return;
            }

            // strip "%"
            parameterName = parameterName.substring(1, parameterName.length() - 1);

            // parameter a always lowercase see #179
            parameterName = parameterName.toLowerCase(Locale.getDefault());
            if (!ContainerCollectionResolver.hasParameterName(lazyServiceCollector, parameterName)) {
                holder.registerProblem(psiElement, "Symfony: Missing Parameter", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
        }

        private ElementPattern<PsiElement> getServiceParameterPattern() {
            ElementPattern<PsiElement> pattern = myServiceParameterPattern != null ? myServiceParameterPattern : YamlElementPatternHelper.getServiceParameterDefinition();
            myServiceParameterPattern = pattern;
            return pattern;
        }

        private ElementPattern<PsiElement> getInsideServiceKeyPattern() {
            ElementPattern<PsiElement> pattern = myInsideServiceKeyPattern != null ? myInsideServiceKeyPattern : YamlElementPatternHelper.getInsideServiceKeyPattern();
            myInsideServiceKeyPattern = pattern;
            return pattern;
        }
    }
}
