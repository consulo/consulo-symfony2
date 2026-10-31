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
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceNamedArgumentExistsInspection extends LocalInspectionTool {
    public static final String INSPECTION_MESSAGE = "Symfony: named argument does not exists";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ServiceNamedArgumentExistsInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Argument does not exists");
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

    private static boolean isSupportedDefinition(PsiElement element) {
        PsiElement context = element.getContext();

        if (context instanceof YAMLKeyValue) {
            // arguments: ['$foobar': '@foo']
            PsiElement yamlMapping = context.getParent();
            if (yamlMapping instanceof YAMLMapping) {
                PsiElement yamlKeyValue = yamlMapping.getParent();
                if (yamlKeyValue instanceof YAMLKeyValue yamlKeyValueParent) {
                    YAMLMapping parentMapping = yamlKeyValueParent.getParentMapping();
                    if (parentMapping != null) {
                        return parentMapping.getKeyValueByKey("factory") == null;
                    }
                }
            }
        }

        return true;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;
        private @Nullable ElementPattern<?> myNamedArgumentPattern;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getNamedArgumentPattern().accepts(element)) {
                if (isSupportedDefinition(element) && ServiceContainerUtil.hasMissingYamlNamedArgumentForInspection(element, getLazyServiceCollector())) {
                    myHolder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }

            super.visitElement(element);
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector() {
            ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                ? myLazyServiceCollector
                : new ContainerCollectionResolver.LazyServiceCollector(myHolder.getProject());
            myLazyServiceCollector = collector;
            return collector;
        }

        private ElementPattern<?> getNamedArgumentPattern() {
            ElementPattern<?> pattern = myNamedArgumentPattern != null ? myNamedArgumentPattern : YamlElementPatternHelper.getNamedArgumentPattern();
            myNamedArgumentPattern = pattern;
            return pattern;
        }
    }
}
