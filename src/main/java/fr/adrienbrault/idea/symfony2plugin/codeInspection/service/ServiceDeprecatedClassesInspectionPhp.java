package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
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
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * consulo-port: upstream nested class ServiceDeprecatedClassesInspection$ServiceDeprecatedClassesInspectionPhp
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceDeprecatedClassesInspectionPhp extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPhpPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ServiceDeprecatedClassesInspectionPhp";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Class in PHP");
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
        private final Supplier<ContainerCollectionResolver.LazyServiceCollector> myServiceCollector;
        private final Supplier<ElementPattern<?>> myAutowireServicePattern;

        private MyPhpPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            myAutowireServicePattern = LazyValue.notNull(() -> PhpElementsUtil.getAttributeNamedArgumentStringPattern(
                ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS,
                "service"
            ));
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof StringLiteralExpression stringLiteralExpression) {
                visitPhpElement(stringLiteralExpression, myHolder);
            }

            super.visitElement(element);
        }

        private void visitPhpElement(StringLiteralExpression psiElement, ProblemsHolder holder) {
            // #[Autowire(service: 'foobar')]
            PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(psiElement);

            if (leafText != null && myAutowireServicePattern.get().accepts(leafText)) {
                String contents = psiElement.getContents();
                if (StringUtils.isNotBlank(contents)) {
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, myServiceCollector);
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, myServiceCollector);
                }

                return;
            }

            MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(psiElement);
            if (methodReference == null || !ServiceContainerUtil.isServiceGetMethod(methodReference)) {
                return;
            }

            String contents = psiElement.getContents();
            if (StringUtils.isNotBlank(contents)) {
                ServiceDeprecatedClassesInspection.ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, myServiceCollector);
                ServiceDeprecatedClassesInspection.ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, myServiceCollector);
            }
        }
    }
}
