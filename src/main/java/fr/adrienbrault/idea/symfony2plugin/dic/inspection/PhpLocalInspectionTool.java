package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
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
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class MissingServiceInspection$PhpLocalInspectionTool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpLocalInspectionTool extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPhpPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "MissingServiceXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Missing Service in XML definition");
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
        return PhpLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPhpPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;
        private @Nullable ElementPattern<?> myAutowireServicePattern;
        private @Nullable ElementPattern<?> myDecoratorAttributePattern;

        MyPhpPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element.getLanguage() == PhpLanguage.INSTANCE && element instanceof StringLiteralExpression stringLiteralExpression) {
                // PHP
                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(stringLiteralExpression);
                if (methodReference != null && ServiceContainerUtil.isServiceGetMethod(methodReference)) {
                    String serviceName = PhpElementsUtil.getFirstArgumentStringValue(methodReference);
                    if (serviceName != null && StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                        myHolder.registerProblem(element, MissingServiceInspection.INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }

                // #[Autowire(service: 'foobar')]
                PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteralExpression);

                boolean isAttributeLeaf = leafText != null && (
                    getAutowireServicePattern().accepts(leafText)
                        || getDecoratorAttributePattern().accepts(leafText)
                );

                if (isAttributeLeaf) {
                    String serviceName = stringLiteralExpression.getContents();
                    if (StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                        myHolder.registerProblem(element, MissingServiceInspection.INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
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

        private ElementPattern<?> getAutowireServicePattern() {
            ElementPattern<?> pattern = myAutowireServicePattern != null
                ? myAutowireServicePattern
                : PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service");
            myAutowireServicePattern = pattern;
            return pattern;
        }

        private ElementPattern<?> getDecoratorAttributePattern() {
            ElementPattern<?> pattern = myDecoratorAttributePattern != null
                ? myDecoratorAttributePattern
                : PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.DECORATOR_ATTRIBUTE_CLASS);
            myDecoratorAttributePattern = pattern;
            return pattern;
        }
    }
}
