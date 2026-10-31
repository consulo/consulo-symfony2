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
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * consulo-port: upstream nested class TaggedExtendsInterfaceClassInspection$TaggedExtendsInterfaceClassInspectionYaml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TaggedExtendsInterfaceClassInspectionYaml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyYamlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TaggedExtendsInterfaceClassYaml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Missing Tag extends/interface statement in Yaml");
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
        private final Supplier<ElementPattern<?>> myServiceIdKeyValuePattern;

        private MyYamlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            mySingleLineClassPattern = LazyValue.notNull(() -> YamlElementPatternHelper.getSingleLineScalarKey("class"));
            myServiceIdKeyValuePattern = LazyValue.notNull(() -> YamlElementPatternHelper.getServiceIdKeyValuePattern());
        }

        @Override
        public void visitElement(PsiElement element) {
            visitYamlElement(element);
            super.visitElement(element);
        }

        private void visitYamlElement(PsiElement psiElement) {
            if (mySingleLineClassPattern.get().accepts(psiElement)) {
                // class: '\Foo'
                String text = PsiElementUtils.trimQuote(psiElement.getText());
                if (StringUtils.isBlank(text)) {
                    return;
                }

                if (!(psiElement.getParent() instanceof YAMLScalar yamlScalar)) {
                    return;
                }
                if (!(yamlScalar.getParent() instanceof YAMLKeyValue classKey)) {
                    return;
                }
                if (!(classKey.getParent() instanceof YAMLCompoundValue yamlCompoundValue)) {
                    return;
                }
                if (!(yamlCompoundValue.getParent() instanceof YAMLKeyValue serviceKeyValue)) {
                    return;
                }

                Set<String> tags = YamlHelper.collectServiceTags(serviceKeyValue);
                if (!tags.isEmpty()) {
                    TaggedExtendsInterfaceClassInspection.registerTaggedProblems(psiElement, tags, text, myHolder, myServiceCollector);
                }
            }
            else if (psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && myServiceIdKeyValuePattern.get().accepts(psiElement.getParent())) {
                // Foobar\Foo: ~
                String text = PsiElementUtils.getText(psiElement);
                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                    YAMLKeyValue yamlKeyValue = psiElement.getParent() instanceof YAMLKeyValue keyValue ? keyValue : null;
                    if (yamlKeyValue != null && YamlHelper.getYamlKeyValue(yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue(yamlKeyValue, "exclude") == null) {
                        Set<String> tags = YamlHelper.collectServiceTags(yamlKeyValue);
                        if (!tags.isEmpty()) {
                            TaggedExtendsInterfaceClassInspection.registerTaggedProblems(psiElement, tags, text, myHolder, myServiceCollector);
                        }
                    }
                }
            }
        }
    }
}
