package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpTemplateGlobalStringGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContainingFile() instanceof PhpFile)) {
            return null;
        }

        PsiElement stringLiteral = psiElement.getContext();
        if(!(stringLiteral instanceof StringLiteralExpression)) {
            return new PsiElement[0];
        }

        if(!PlatformPatterns.or(
            PlatformPatterns
                .psiElement(StringLiteralExpression.class)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE),
            PlatformPatterns
                .psiElement(StringLiteralExpression.class)
                .withText(PlatformPatterns.or(
                    PlatformPatterns.string().endsWith("twig'"),
                    PlatformPatterns.string().endsWith("twig\"")
                ))
                .withLanguage(PhpLanguage.INSTANCE)
            ).accepts(stringLiteral)) {

            return new PsiElement[0];
        }

        String templateName = ((StringLiteralExpression) stringLiteral).getContents();
        if(StringUtils.isBlank(templateName)) {
            return new PsiElement[0];
        }

        // file and directory navigation:
        // foo.html.twig, foobar/foo.html.twig
        return TwigUtil.getTemplateNavigationOnOffset(psiElement.getProject(), templateName, i - psiElement.getTextRange().getStartOffset()).toArray(new PsiElement[0]);
    }

    @Override
    public LocalizeValue getActionText(DataContext dataContext) {
        return LocalizeValue.empty();
    }

}
