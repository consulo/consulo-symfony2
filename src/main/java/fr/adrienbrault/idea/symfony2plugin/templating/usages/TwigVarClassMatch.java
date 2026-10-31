package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One resolved Twig `@var` class usage together with the exact class-name range inside the comment token.
 *
 * Examples:
 * `{# @var cardSuite \Foo\CardSuite #}` -> range of `\Foo\CardSuite`
 * `{# @var \Foo\CardSuite cardSuite #}` -> range of `\Foo\CardSuite`
 */
public final class TwigVarClassMatch {
    private final PhpClass myTargetClass;
    private final TextRange myRangeInElement;

    public TwigVarClassMatch(PhpClass targetClass, TextRange rangeInElement) {
        myTargetClass = targetClass;
        myRangeInElement = rangeInElement;
    }

    public PhpClass getTargetClass() {
        return myTargetClass;
    }

    public TextRange getRangeInElement() {
        return myRangeInElement;
    }

    public PhpClass component1() {
        return myTargetClass;
    }

    public TextRange component2() {
        return myRangeInElement;
    }

    public TwigVarClassMatch copy(PhpClass targetClass, TextRange rangeInElement) {
        return new TwigVarClassMatch(targetClass, rangeInElement);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigVarClassMatch)) {
            return false;
        }
        TwigVarClassMatch that = (TwigVarClassMatch) o;
        return Objects.equals(myTargetClass, that.myTargetClass) && Objects.equals(myRangeInElement, that.myRangeInElement);
    }

    @Override
    public int hashCode() {
        return myTargetClass.hashCode() * 31 + myRangeInElement.hashCode();
    }

    @Override
    public String toString() {
        return "TwigVarClassMatch(targetClass=" + myTargetClass + ", rangeInElement=" + myRangeInElement + ")";
    }
}
