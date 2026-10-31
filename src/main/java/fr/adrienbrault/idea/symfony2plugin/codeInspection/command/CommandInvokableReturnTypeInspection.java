package fr.adrienbrault.idea.symfony2plugin.codeInspection.command;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturnType;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Inspection to suggest adding an int return type to __invoke() method in Symfony Commands.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * should declare int as return type for the exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class CommandInvokableReturnTypeInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Consider adding int return type to command __invoke()";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PhpClass phpClass) {
                    visitPhpClass(phpClass, holder);
                }
                super.visitElement(element);
            }
        };
    }

    private void visitPhpClass(PhpClass phpClass, ProblemsHolder holder) {
        if (!hasAsCommandAttribute(phpClass)) {
            return;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return;
        }

        if (!hasIntReturnType(invokeMethod)) {
            PsiElement problemElement = invokeMethod.getNameIdentifier();
            holder.registerProblem(
                problemElement != null ? problemElement : invokeMethod,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            );
        }
    }

    private boolean hasAsCommandAttribute(PhpClass phpClass) {
        return !phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty();
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

    @Override
    public String getShortName() {
        return "CommandInvokableReturnTypeInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Consider adding int return type to command __invoke()");
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
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
