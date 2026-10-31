package fr.adrienbrault.idea.symfony2plugin.completion;

import com.jetbrains.php.lang.psi.PhpFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * Triggers auto-popup completion after typing '#' character in PHP files
 * when positioned before a public method or class (for PHP attributes like #[Route()])
 *
 * Moved from nested class PhpAttributeCompletionPopupHandlerCompletionConfidence.PhpAttributeAutoPopupHandler
 * (Consulo cannot register nested extension classes)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpAttributeAutoPopupHandler extends TypedHandlerDelegate {
    @Override
    public Result checkAutoPopup(char charTyped, Project project, Editor editor, PsiFile file) {
        if (charTyped != '#' || !(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
            return Result.CONTINUE;
        }


        // Check if we're in a class context
        int offset = editor.getCaretModel().getOffset();
        if (!(file.findElementAt(offset - 2) instanceof PsiWhiteSpace) && !(file.findElementAt(offset - 1) instanceof PsiWhiteSpace)) {
            return Result.CONTINUE;
        }

        PsiElement element = file.findElementAt(offset - 1);
        if (element == null) {
            return Result.CONTINUE;
        }

        // Check if we should provide attribute completions for this context
        // (controller class, twig component, twig extension, etc.)
        if (!PhpAttributeScopeValidator.shouldProvideAttributeCompletions(element, project)) {
            return Result.CONTINUE;
        }

        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return Result.STOP;
    }
}
