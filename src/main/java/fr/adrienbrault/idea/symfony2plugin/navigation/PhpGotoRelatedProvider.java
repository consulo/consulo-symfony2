package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ControllerMethodLineMarkerProvider;

import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpGotoRelatedProvider extends GotoRelatedProvider {
    @Override
    public List<? extends GotoRelatedItem> getItems(PsiElement psiElement) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return Collections.emptyList();
        }

        if (psiElement.getLanguage() != PhpLanguage.INSTANCE) {
            return Collections.emptyList();
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null || !method.getAccess().isPublic()) {
            return Collections.emptyList();
        }

        return ControllerMethodLineMarkerProvider.getGotoRelatedItems(method);
    }
}
