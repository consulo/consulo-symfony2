package fr.adrienbrault.idea.symfony2plugin.util.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.ast.IElementType;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementAssertUtil {

    public static boolean isNotNullAndIsElementType(@Nullable PsiElement psiElement, IElementType... iElementType) {
        return psiElement != null && Arrays.asList(iElementType).contains(psiElement.getNode().getElementType());
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> @Nullable T getParentOfTypeOrNull(PsiElement element, Class<T> aClass) {
        PsiElement parent = element.getParent();
        return aClass.isInstance(parent) ? (T) parent : null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiNamedElement> @Nullable T getParentOfTypeWithNameOrNull(PsiElement element, Class<T> aClass, String name) {
        PsiElement parent = element.getParent();
        if(!aClass.isInstance(parent) || !(parent instanceof PsiNamedElement) || !name.equals(((PsiNamedElement) parent).getName())) {
            return null;
        }

        return (T) parent;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> @Nullable T getInstanceOfOrNull(@Nullable PsiElement element, Class<T> aClass) {
        if(element == null) return null;
        return aClass.isInstance(element) ? (T) element : null;
    }

}
