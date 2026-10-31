package fr.adrienbrault.idea.symfony2plugin.dic.container.dict;

import com.jetbrains.php.lang.psi.elements.Method;
import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceTypeHint {
    private final Method method;

    private final int index;

    private final PsiElement psiElement;

    public ServiceTypeHint(Method method, int index, PsiElement psiElement) {
        this.method = method;
        this.index = index;
        this.psiElement = psiElement;
    }

    public Method getMethod() {
        return method;
    }

    public int getIndex() {
        return index;
    }

    public PsiElement getElement() {
        return psiElement;
    }
}
