package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlock {
    private final String name;

    private final PsiElement target;

    public TwigBlock(String name, PsiElement target) {
        this.name = name;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public PsiElement getTarget() {
        return target;
    }
}

