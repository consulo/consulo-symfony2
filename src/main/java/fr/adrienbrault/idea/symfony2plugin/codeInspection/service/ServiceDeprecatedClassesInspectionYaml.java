package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * consulo-port: upstream nested class ServiceDeprecatedClassesInspection$ServiceDeprecatedClassesInspectionYaml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceDeprecatedClassesInspectionYaml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyYamlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ServiceDeprecatedClassesInspectionYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Class in Yaml");
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
        private final Supplier<ContainerCollectionResolver.LazyServiceCollector> myServiceCollector;
        private final Supplier<ElementPattern<PsiElement>> mySingleLineClassPattern;

        private MyYamlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            mySingleLineClassPattern = LazyValue.notNull(() -> YamlElementPatternHelper.getSingleLineScalarKey("class"));
        }

        @Override
        public void visitElement(PsiElement element) {
            visitYamlElement(element);
            super.visitElement(element);
        }

        private void visitYamlElement(PsiElement element) {
            if (mySingleLineClassPattern.get().accepts(element)) {
                // class: '\Foo'
                String text = PsiElementUtils.trimQuote(element.getText());
                if (StringUtils.isNotBlank(text)) {
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachDeprecatedProblem(element, text, myHolder, myServiceCollector);
                }
            }
            else if (element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                // @service
                String text = element.getText();
                if (StringUtils.isNotBlank(text) && text.startsWith("@")) {
                    String serviceName = text.substring(1);
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachDeprecatedProblem(element, serviceName, myHolder, myServiceCollector);
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachServiceDeprecatedProblem(element, serviceName, myHolder, myServiceCollector);
                }
            }
        }
    }
}
