package fr.adrienbrault.idea.symfony2plugin.completion;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * Tells IntelliJ that completion should definitely run after "#" in PHP classes
 * This is needed for auto-popup to work for PHP attributes
 *
 * Moved from nested class PhpAttributeCompletionPopupHandlerCompletionConfidence.PhpAttributeCompletionConfidence
 * (Consulo cannot register nested extension classes)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpAttributeCompletionConfidence extends CompletionConfidence {
    @Override
    public ThreeState shouldSkipAutopopup(PsiElement contextElement, PsiFile psiFile, int offset) {
        if (offset <= 0 || !(psiFile instanceof PhpFile)) {
            return ThreeState.UNSURE;
        }

        Project project = psiFile.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return ThreeState.UNSURE;
        }

        // Check if there's a "#" before the cursor in the document
        CharSequence documentText = psiFile.getViewProvider().getContents();
        if (documentText.charAt(offset - 1) == '#' && psiFile.findElementAt(offset - 2) instanceof PsiWhiteSpace) {
            // Check if we should provide attribute completions for this context
            // (controller class, twig component, twig extension, etc.)
            if (PhpAttributeScopeValidator.shouldProvideAttributeCompletions(contextElement, project)) {
                return ThreeState.NO;
            }
        }

        return ThreeState.UNSURE;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
