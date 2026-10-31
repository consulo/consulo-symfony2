package fr.adrienbrault.idea.symfony2plugin.form.intention;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.FormStringToClassConstantIntention", fileExtensions = "php", categories = "Symfony")
public class FormStringToClassConstantIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        if (!(psiElement.getParent() instanceof StringLiteralExpression parent)) {
            return;
        }

        try {
            FormUtil.replaceFormStringAliasWithClassConstant(parent);
        }
        catch (Exception e) {
            IdeHelper.showErrorHintIfAvailable(editor, e.getMessage());
        }
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>PHP</language>
        if (!psiElement.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        if (!(psiElement.getParent() instanceof StringLiteralExpression parent)) {
            return false;
        }

        if (StringUtils.isBlank(parent.getContents())) {
            return false;
        }

        return new MethodMatcher.StringParameterMatcher(parent, 1)
            .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
            .match() != null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: use FormType class constant");
    }
}
