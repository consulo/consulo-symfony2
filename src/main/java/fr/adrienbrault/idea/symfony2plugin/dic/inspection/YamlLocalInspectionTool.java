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

/**
 * consulo-port: was the nested class MissingServiceInspection$YamlLocalInspectionTool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlLocalInspectionTool extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyYamlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "MissingServiceYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Missing Service in yaml definition");
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
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;
        private @Nullable ElementPattern<?> myServiceDefinitionPattern;
        private @Nullable ElementPattern<?> myInsideServiceKeyPattern;

        MyYamlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getServiceDefinitionPattern().accepts(element) && getInsideServiceKeyPattern().accepts(element)) {
                String serviceName = YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(element));

                // dont mark "@", "@?", "@@" escaping and expressions
                if (serviceName.length() > 2 && !serviceName.startsWith("=") && !serviceName.startsWith("@") && !hasService(serviceName)) {
                    myHolder.registerProblem(element, MissingServiceInspection.INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }

            super.visitElement(element);
        }

        private boolean hasService(String serviceName) {
            ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                ? myLazyServiceCollector
                : new ContainerCollectionResolver.LazyServiceCollector(myHolder.getProject());
            myLazyServiceCollector = collector;

            return ContainerCollectionResolver.hasServiceName(collector, serviceName);
        }

        private ElementPattern<?> getServiceDefinitionPattern() {
            ElementPattern<?> pattern = myServiceDefinitionPattern != null ? myServiceDefinitionPattern : YamlElementPatternHelper.getServiceDefinition();
            myServiceDefinitionPattern = pattern;
            return pattern;
        }

        private ElementPattern<?> getInsideServiceKeyPattern() {
            ElementPattern<?> pattern = myInsideServiceKeyPattern != null ? myInsideServiceKeyPattern : YamlElementPatternHelper.getInsideServiceKeyPattern();
            myInsideServiceKeyPattern = pattern;
            return pattern;
        }
    }
}
