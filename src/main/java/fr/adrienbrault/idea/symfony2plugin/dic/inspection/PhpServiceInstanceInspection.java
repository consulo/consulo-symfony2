package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
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
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.PhpServiceSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * PHP array-style and fluent service config inspection for wrong service instance references.
 *
 * <pre>
 * MyService::class => [
 *     'arguments' => [service('<caret>'), '@<caret>'],
 * ]
 *
 * $services->set(MyService::class)->args([service('<caret>')]);
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpServiceInstanceInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "PhpServiceInstanceInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("PHP: Wrong Service Instance");
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

    private static void registerPhpServiceInstanceProblem(
        PsiElement argument,
        ProblemsHolder holder,
        ServiceTypeHint typeHint,
        ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector
    ) {
        String serviceId;
        if (argument instanceof StringLiteralExpression stringLiteralExpression) {
            String contents = stringLiteralExpression.getContents();
            if (StringUtils.isBlank(contents)) {
                return;
            }
            // Strip '@' prefix for raw '@service_id' strings
            serviceId = contents.startsWith("@") ? contents.substring(1) : contents;
        }
        else if (argument instanceof ClassConstantReference classConstantReference) {
            serviceId = PhpElementsUtil.getClassConstantPhpFqn(classConstantReference);
        }
        else {
            return;
        }

        if (serviceId == null || StringUtils.isBlank(serviceId)) {
            return;
        }

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceId, lazyServiceCollector);
        if (serviceClass == null) {
            return;
        }

        Parameter[] parameters = typeHint.getMethod().getParameters();
        int index = typeHint.getIndex();
        if (index >= parameters.length) {
            return;
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), parameters[index].getDeclaredType().toString());
        if (expectedClass == null) {
            return;
        }

        if (!PhpElementsUtil.isInstanceOf(serviceClass, expectedClass)) {
            holder.registerProblem(
                argument,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new PhpServiceSuggestIntentionAction(expectedClass.getFQN(), argument)
            );
        }
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (psiElement instanceof StringLiteralExpression stringLiteralExpression) {
                String contents = stringLiteralExpression.getContents();
                if (StringUtils.isNotBlank(contents)) {
                    visitArgument(psiElement);
                }
            }
            else if (psiElement instanceof ClassConstantReference) {
                visitArgument(psiElement);
            }

            super.visitElement(psiElement);
        }

        private void visitArgument(PsiElement argument) {
            ContainerCollectionResolver.LazyServiceCollector collector = getLazyServiceCollector(myHolder.getProject());

            ServiceTypeHint typeHint = ServiceContainerUtil.getPhpArrayConstructorTypeHint(argument, collector);
            if (typeHint == null) {
                typeHint = ServiceContainerUtil.getPhpFluentConstructorTypeHint(argument, collector);
            }

            if (typeHint == null) {
                return;
            }

            registerPhpServiceInstanceProblem(argument, myHolder, typeHint, collector);
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
            ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                ? myLazyServiceCollector
                : new ContainerCollectionResolver.LazyServiceCollector(project);
            myLazyServiceCollector = collector;
            return collector;
        }
    }
}
