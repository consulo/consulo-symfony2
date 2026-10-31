package fr.adrienbrault.idea.symfony2plugin.util.psi;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.ArrayValueWithKeyAndMethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.ArrayValueWithKeyAndNewExpressionMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.psi.matcher.NamedValueWithKeyAndNewExpressionMatcher;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpPsiMatcher {

    public static ArrayValueWithKeyAndMethod.@Nullable Result match(PsiElement psiElement, ArrayValueWithKeyAndMethod.Matcher matcher) {
        return ArrayValueWithKeyAndMethod.match(psiElement, matcher);
    }

    /**
     * new Foo(['message' => '<caret>'])
     */
    public static ArrayValueWithKeyAndNewExpressionMatcher.@Nullable Result match(PsiElement psiElement, ArrayValueWithKeyAndNewExpressionMatcher.Matcher matcher) {
        return ArrayValueWithKeyAndNewExpression.match(psiElement, matcher);
    }

    /**
     * new Foo(message: '<caret>')
     */
    public static NamedValueWithKeyAndNewExpression.@Nullable Result match(PsiElement psiElement, NamedValueWithKeyAndNewExpression.Matcher matcher) {
        return NamedValueWithKeyAndNewExpression.match(psiElement, matcher);
    }

    final public static class ArrayValueWithKeyAndMethod extends ArrayValueWithKeyAndMethodMatcher {}
    final public static class ArrayValueWithKeyAndNewExpression extends ArrayValueWithKeyAndNewExpressionMatcher {}
    final public static class NamedValueWithKeyAndNewExpression extends NamedValueWithKeyAndNewExpressionMatcher {}
}
