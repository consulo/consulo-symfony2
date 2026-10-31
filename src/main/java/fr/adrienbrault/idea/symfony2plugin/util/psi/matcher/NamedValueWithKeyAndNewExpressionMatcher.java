package fr.adrienbrault.idea.symfony2plugin.util.psi.matcher;

import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NamedValueWithKeyAndNewExpressionMatcher {

    /**
     * new Foobar(
     *   route: '<caret>',
     * );
     */
    public static PsiElementPattern.Capture<PsiElement> pattern() {
        return PhpPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class).afterLeafSkipping(PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PhpTokenTypes.opCOLON)
            ), PlatformPatterns.psiElement(PhpTokenTypes.IDENTIFIER)).withParent(PlatformPatterns.psiElement(ParameterList.class))
        );
    }

    /**
     * new Foobar(
     *   route: '<caret>',
     * );
     */
    public static @Nullable Result match(PsiElement psiElement, Matcher matcher) {
        PsiElement parameterList = psiElement.getParent();
        if (parameterList instanceof ParameterList) {
            PsiElement newExpression = parameterList.getParent();
            if (newExpression instanceof NewExpression) {
                for (NewExpressionCall newExpressionCall : matcher.getNewExpressionCalls()) {
                    PsiElement colon = PsiTreeUtil.prevCodeLeaf(psiElement);
                    if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
                        continue;
                    }

                    PsiElement identifier = PsiTreeUtil.prevCodeLeaf(colon);
                    if (identifier == null || identifier.getNode().getElementType() != PhpTokenTypes.IDENTIFIER || !newExpressionCall.getNamedArgument().equals(identifier.getText())) {
                        continue;
                    }

                    if (!PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) newExpression, newExpressionCall.getClazz())) {
                        continue;
                    }

                    return new Result(newExpressionCall, (NewExpression) newExpression, identifier);
                }
            }
        }

        return null;
    }

    public static class Matcher {
        private final NewExpressionCall[] classes;

        public Matcher(String namedArgument, String clazz) {
            this.classes = new NewExpressionCall[] { new NewExpressionCall(clazz, namedArgument)};
        }

        public Matcher(NewExpressionCall... classes) {
            this.classes = classes;
        }

        public NewExpressionCall[] getNewExpressionCalls() {
            return classes;
        }
    }

    public static class Result {
        private final NewExpression newExpression;

        private final NewExpressionCall expressionCall;

        private final PsiElement arrayKey;

        public Result(NewExpressionCall expressionCall, NewExpression newExpression, PsiElement arrayKey) {
            this.expressionCall = expressionCall;
            this.newExpression = newExpression;
            this.arrayKey = arrayKey;
        }

        public NewExpression getNewExpression() {
            return newExpression;
        }

        public NewExpressionCall getExpressionCall() {
            return expressionCall;
        }

        public PsiElement getArrayKey() {
            return arrayKey;
        }
    }

    public static class NewExpressionCall {
        private final String clazz;

        private final String namedArgument;

        public NewExpressionCall(String clazz, String namedArgument) {
            this.clazz = clazz;
            this.namedArgument = namedArgument;
        }

        public String getClazz() {
            return clazz;
        }

        public String getNamedArgument() {
            return namedArgument;
        }
    }
}
