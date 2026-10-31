package fr.adrienbrault.idea.symfony2plugin.config;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpClassServiceGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
            return new PsiElement[0];
        }

        return ServiceIndexUtil.findServiceDefinitions((PhpClass) psiElement.getContext());
    }

    @Override
    public LocalizeValue getActionText(DataContext context) {
        return LocalizeValue.empty();
    }

}
