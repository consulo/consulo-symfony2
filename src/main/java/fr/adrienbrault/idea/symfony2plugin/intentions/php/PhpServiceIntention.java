package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceGenerateActionKt;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.PhpServiceIntention", fileExtensions = "yaml", categories = "Symfony")
public class PhpServiceIntention extends PsiElementBaseIntentionAction {
    @Override
    public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
        return null;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        Method parentByCondition = PhpPsiUtil.getParentByCondition(psiElement, true, Method.INSTANCEOF, null);
        if (parentByCondition == null) {
            return;
        }

        PhpClass phpClass = PhpPsiUtil.getParentByCondition(psiElement, true, PhpClass.INSTANCEOF, null);
        if (phpClass == null) {
            return;
        }

        ServiceGenerateActionKt.invokeServiceGenerator(project, phpClass.getContainingFile(), phpClass, editor);
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

        Method parentByCondition = PhpPsiUtil.getParentByCondition(psiElement, true, Method.INSTANCEOF, null);
        if (parentByCondition == null) {
            return false;
        }

        return PhpPsiUtil.<PhpClass>getParentByCondition(psiElement, true, PhpClass.INSTANCEOF, null) != null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Generate Symfony service");
    }
}
