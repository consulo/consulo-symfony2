package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import org.jspecify.annotations.Nullable;

/**
 * Top-level functions of upstream ServiceGenerateAction.kt
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class ServiceGenerateActionKt {
    private ServiceGenerateActionKt() {
    }

    @RequiredUIAccess
    public static void invokeServiceGenerator(Project project, PsiFile file, PhpClass phpClass, @Nullable Editor editor) {
        if (editor != null) {
            SymfonyCreateService.create(project, file, phpClass, editor);
        }
    }
}
