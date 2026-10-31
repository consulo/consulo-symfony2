package fr.adrienbrault.idea.symfony2plugin.doctrine.intention;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.DoctrineRepositoryClassConstantIntention", fileExtensions = "php", categories = "Symfony")
public class DoctrineRepositoryClassConstantIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        if (!(psiElement.getParent() instanceof StringLiteralExpression parent)) {
            return;
        }

        try {
            PhpClass phpClass = EntityHelper.resolveShortcutName(project, parent.getContents());
            if (phpClass == null) {
                throw new Exception("Can not resolve model class");
            }
            PhpElementsUtil.replaceElementWithClassConstant(phpClass, parent);
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

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        if (!(psiElement.getParent() instanceof StringLiteralExpression parent)) {
            return false;
        }

        if (StringUtils.isBlank(parent.getContents())) {
            return false;
        }

        return new MethodMatcher.StringParameterMatcher(parent, 0)
            .withSignature(SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES)
            .withSignature("Doctrine\\Persistence\\ObjectManager", "find")
            .withSignature("Doctrine\\Common\\Persistence\\ObjectManager", "find") // @TODO: missing somewhere
            .match() != null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Doctrine: use class constant");
    }
}
