package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.YamlSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jspecify.annotations.Nullable;

/**
 * foo:
 *  class: Foo
 *  arguments: [@<caret>]
 *  arguments:
 *      - @<caret>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlXmlServiceInstanceInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "YamlXmlServiceInstanceInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Yaml: Wrong Service Instance");
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

    private static void registerInstanceProblem(PsiElement psiElement, ProblemsHolder holder, int parameterIndex, Method constructor, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        String serviceName = getServiceName(psiElement);
        if (StringUtils.isBlank(serviceName)) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), getServiceName(psiElement), lazyServiceCollector);
        if (serviceParameterClass == null) {
            return;
        }

        Parameter[] constructorParameter = constructor.getParameters();
        if (parameterIndex >= constructorParameter.length) {
            return;
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), constructorParameter[parameterIndex].getDeclaredType().toString());
        if (expectedClass == null) {
            return;
        }

        if (!PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.registerProblem(
                psiElement,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                new YamlSuggestIntentionAction(expectedClass.getFQN(), psiElement)
            );
        }
    }

    private static String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;

        private @Nullable ElementPattern<PsiElement> myInsideServicesPattern;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            // only match inside service definitions
            if (YamlHelper.isStringValue(psiElement) && getInsideServicesPattern().accepts(psiElement)) {
                visitConstructor(psiElement);
                visitCall(psiElement);
            }

            super.visitElement(psiElement);
        }

        /**
         * class: FooClass
         * tags:
         *  - [ setFoo, [@args_bar] ]
         */
        private void visitCall(PsiElement psiElement) {
            PsiElement yamlScalar = psiElement.getContext();
            if (!(yamlScalar instanceof YAMLScalar)) {
                return;
            }

            YamlHelper.visitServiceCallArgument((YAMLScalar) yamlScalar, visitor -> {
                PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(myHolder.getProject(), visitor.getClassName(), getLazyServiceCollector(myHolder.getProject()));
                if (serviceClass == null) {
                    return;
                }

                Method method = serviceClass.findMethodByName(visitor.getMethod());
                if (method == null) {
                    return;
                }

                registerInstanceProblem(psiElement, myHolder, visitor.getParameterIndex(), method, getLazyServiceCollector(myHolder.getProject()));
            });
        }

        /**
         * foo:
         *  class: Foo
         *  arguments: [@<caret>]
         *  arguments:
         *      - @<caret>
         */
        private void visitConstructor(PsiElement psiElement) {
            ServiceTypeHint methodTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(psiElement, getLazyServiceCollector(myHolder.getProject()));
            if (methodTypeHint == null) {
                return;
            }

            registerInstanceProblem(psiElement, myHolder, methodTypeHint.getIndex(), methodTypeHint.getMethod(), getLazyServiceCollector(myHolder.getProject()));
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
            ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                ? myLazyServiceCollector
                : new ContainerCollectionResolver.LazyServiceCollector(project);
            myLazyServiceCollector = collector;
            return collector;
        }

        private ElementPattern<PsiElement> getInsideServicesPattern() {
            ElementPattern<PsiElement> pattern = myInsideServicesPattern != null ? myInsideServicesPattern : YamlElementPatternHelper.getInsideKeyValue("services");
            myInsideServicesPattern = pattern;
            return pattern;
        }
    }
}
