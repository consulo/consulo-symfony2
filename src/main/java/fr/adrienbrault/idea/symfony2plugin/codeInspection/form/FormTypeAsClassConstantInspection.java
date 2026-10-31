package fr.adrienbrault.idea.symfony2plugin.codeInspection.form;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class FormTypeAsClassConstantInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Use fully-qualified class name (FQCN)";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "FormTypeAsClassConstant";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Fully-qualified class name (FQCN) in form types");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Form");
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

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private final Supplier<Boolean> myIsVersionGreaterThenEquals;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myIsVersionGreaterThenEquals = LazyValue.notNull(() -> SymfonyUtil.isVersionGreaterThenEquals(holder.getProject(), "2.8"));
        }

        @Override
        public void visitElement(PsiElement element) {
            if (!(element instanceof MethodReference methodReference) ||
                !"add".equals(methodReference.getName()) && !"create".equals(methodReference.getName()) ||
                !PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Form\\FormBuilderInterface")
            ) {
                super.visitElement(element);
                return;
            }

            if (!myIsVersionGreaterThenEquals.get()) {
                return;
            }

            PsiElement[] parameters = methodReference.getParameters();
            if (parameters.length < 2) {
                super.visitElement(element);
                return;
            }

            PsiElement formType = parameters[1];
            if (!(formType instanceof StringLiteralExpression stringLiteralExpression) || stringLiteralExpression.getContents().contains("\\")) {
                super.visitElement(element);
                return;
            }

            myHolder.registerProblem(
                formType,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING,
                new MyLocalQuickFix(formType)
            );

            super.visitElement(element);
        }

        private static class MyLocalQuickFix extends LocalQuickFixOnPsiElement {
            private MyLocalQuickFix(PsiElement element) {
                super(element);
            }

            @Override
            public LocalizeValue getText() {
                return LocalizeValue.localizeTODO("Use class constant");
            }

            @Override
            public void invoke(Project project, PsiFile psiFile, PsiElement psiElement, PsiElement psiElement1) {
                if (!(getStartElement() instanceof StringLiteralExpression formType)) {
                    return;
                }

                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(formType);
                }
                catch (Exception ignored) {
                }
            }
        }
    }
}
