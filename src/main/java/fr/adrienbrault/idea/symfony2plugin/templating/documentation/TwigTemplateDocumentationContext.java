package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import consulo.language.psi.PsiElement;

import java.util.Objects;

/**
 * Former Kotlin {@code internal data class TwigTemplateDocumentationContext(val name: String, val anchor: PsiElement)}
 * of TwigTemplateDocumentationTargetProvider.kt
 */
public final class TwigTemplateDocumentationContext {
    private final String myName;
    private final PsiElement myAnchor;

    public TwigTemplateDocumentationContext(String name, PsiElement anchor) {
        myName = name;
        myAnchor = anchor;
    }

    public String getName() {
        return myName;
    }

    public PsiElement getAnchor() {
        return myAnchor;
    }

    public String component1() {
        return myName;
    }

    public PsiElement component2() {
        return myAnchor;
    }

    public TwigTemplateDocumentationContext copy(String name, PsiElement anchor) {
        return new TwigTemplateDocumentationContext(name, anchor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigTemplateDocumentationContext that)) {
            return false;
        }
        return Objects.equals(myName, that.myName) && Objects.equals(myAnchor, that.myAnchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myAnchor);
    }

    @Override
    public String toString() {
        return "TwigTemplateDocumentationContext(name=" + myName + ", anchor=" + myAnchor + ")";
    }
}
