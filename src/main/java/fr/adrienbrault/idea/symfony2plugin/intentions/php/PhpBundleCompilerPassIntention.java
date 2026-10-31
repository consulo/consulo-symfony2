package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.PhpBundleCompilerPassIntention", fileExtensions = "php", categories = "Symfony")
public class PhpBundleCompilerPassIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        if (!(psiElement.getContainingFile() instanceof PhpFile)) {
            return;
        }

        PhpClass phpClass = PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass.class));
        if (phpClass == null) {
            return;
        }

        PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor);
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>PHP</language>
        if (!psiElement.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        if (!(psiElement.getContainingFile() instanceof PhpFile)) {
            return false;
        }

        return PhpBundleFileFactory.getPhpClassForCreateCompilerScope(PsiTreeUtil.getParentOfType(psiElement, PhpClass.class)) != null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Create CompilerPass");
    }
}
