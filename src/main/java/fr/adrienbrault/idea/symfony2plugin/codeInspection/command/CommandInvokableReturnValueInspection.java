package fr.adrienbrault.idea.symfony2plugin.codeInspection.command;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.PhpReturnType;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;

/**
 * Inspection to detect invokable Symfony Commands that return non-integer values.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * must return an integer exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * This inspection checks the actual return statements inside the method body.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class CommandInvokableReturnValueInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Command must return an integer value";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PhpReturn phpReturn) {
                    visitPhpReturn(phpReturn, holder);
                }
                super.visitElement(element);
            }
        };
    }

    private void visitPhpReturn(PhpReturn phpReturn, ProblemsHolder holder) {
        Method method = PsiTreeUtil.getParentOfType(phpReturn, Method.class);
        if (method == null) {
            return;
        }

        if (!"__invoke".equals(method.getName())) {
            return;
        }

        if (hasIntReturnType(method)) {
            return;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return;
        }

        if (!hasAsCommandAttribute(phpClass)) {
            return;
        }

        PsiElement argument = phpReturn.getArgument();
        if (argument == null) {
            holder.registerProblem(phpReturn, MESSAGE, ProblemHighlightType.WARNING);
            return;
        }

        if (!isIntegerType(argument, holder)) {
            holder.registerProblem(argument, MESSAGE, ProblemHighlightType.WARNING);
        }
    }

    private boolean hasIntReturnType(Method method) {
        PsiElement child = PhpPsiUtil.getChildByCondition(method, PhpReturnType.INSTANCEOF);
        if (!(child instanceof PhpReturnType returnType)) {
            return false;
        }

        PhpType type = returnType.getType();
        if (type.isEmpty()) {
            return false;
        }

        for (String typeName : type.getTypes()) {
            String normalized = typeName.replace("\\", "").toLowerCase(Locale.ROOT);
            if (normalized.equals("int") || normalized.equals("integer")) {
                return true;
            }
        }

        return false;
    }

    private boolean isIntegerType(PsiElement element, ProblemsHolder holder) {
        if (!(element instanceof PhpTypedElement phpTypedElement)) {
            return false;
        }

        PhpType type = phpTypedElement.getType();
        if (type.isEmpty()) {
            return false;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(holder.getProject());
        PhpType completedType = phpIndex.completeType(holder.getProject(), type, new HashSet<>());

        for (String typeName : completedType.getTypes()) {
            String normalized = typeName.replace("\\", "").toLowerCase(Locale.ROOT);
            if (normalized.equals("int") || normalized.equals("integer")) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAsCommandAttribute(PhpClass phpClass) {
        return !phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty();
    }

    @Override
    public String getShortName() {
        return "CommandInvokableReturnValueInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Command must return an integer value");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Command");
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
}
