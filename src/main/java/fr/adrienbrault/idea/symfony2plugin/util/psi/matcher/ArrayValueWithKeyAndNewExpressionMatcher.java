package fr.adrienbrault.idea.symfony2plugin.util.psi.matcher;

import consulo.util.lang.Pair;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ArrayValueWithKeyAndNewExpressionMatcher {

    /**
     * $menu->addChild([
     *   'route' => '<caret>',
     * ]);
     */
    public static PsiElementPattern.Capture<PsiElement> pattern() {
        return PhpPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).inside(ParameterList.class)
            )
        );
    }

    /**
     * A array value inside array and method reference
     *
     * $menu->addChild([
     *   'route' => '<caret>',
     * ]);
     */
    public static @Nullable Result match(PsiElement psiElement, Matcher matcher) {
        PsiElement arrayValue = psiElement.getParent();
        if(PsiElementAssertUtil.isNotNullAndIsElementType(arrayValue, PhpElementTypes.ARRAY_VALUE)) {
            PsiElement arrayHashElement = arrayValue.getParent();
            if(arrayHashElement instanceof ArrayHashElement) {
                PhpPsiElement arrayKey = ((ArrayHashElement) arrayHashElement).getKey();
                if(arrayKey != null && ArrayUtils.contains(matcher.getArrayKeys(), PhpElementsUtil.getStringValue(arrayKey))) {
                    PsiElement arrayCreationExpression = arrayHashElement.getParent();
                    if(arrayCreationExpression instanceof ArrayCreationExpression) {
                        Pair<NewExpressionCall, NewExpression> matchPair = matchesMethodCall((ArrayCreationExpression) arrayCreationExpression, matcher.getNewExpressionCalls());
                        if(matchPair != null) {
                            return new Result(matchPair.getFirst(), matchPair.getSecond(), arrayKey);
                        }
                    }
                }
            }
        }

        return null;
    }

    private static @Nullable Pair<NewExpressionCall, NewExpression> matchesMethodCall(ArrayCreationExpression arrayCreationExpression, NewExpressionCall[] classes) {

        // get parameter, we less then 0 we are not inside method reference
        int parameterIndex = PsiElementUtils.getParameterIndexValue(arrayCreationExpression);
        if(parameterIndex < 0) {
            return null;
        }

        // filter index
        List<NewExpressionCall> filter = ContainerUtil.filter(classes, expressionCall ->
            expressionCall.getIndex() == parameterIndex
        );

        if(filter.isEmpty()) {
            return null;
        }

        PsiElement parameterList = arrayCreationExpression.getParent();
        if(!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement newExpression = parameterList.getParent();
        if (!(newExpression instanceof NewExpression)) {
            return null;
        }

        for (NewExpressionCall aClass : filter) {
            if(PhpElementsUtil.getNewExpressionPhpClassWithInstance((NewExpression) newExpression, aClass.getClazz()) == null) {
                continue;
            }

            return Pair.create(aClass, (NewExpression) newExpression);
        }

        return null;
    }

    public static class Matcher {
        private final String[] arrayKeys;

        private final NewExpressionCall[] classes;

        public Matcher(String[] arrayKeys, NewExpressionCall... classes) {
            this.arrayKeys = arrayKeys;
            this.classes = classes;
        }

        public Matcher(String arrayKeys, NewExpressionCall... classes) {
            this(new String[] {arrayKeys}, classes);
        }

        public Matcher(String arrayKeys, String clazz) {
            this(new String[] {arrayKeys}, new NewExpressionCall(clazz, 0));
        }

        public String[] getArrayKeys() {
            return arrayKeys;
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

        private int index;

        public NewExpressionCall(String clazz) {
            this(clazz, 0);
        }

        public NewExpressionCall(String clazz, int index) {
            this.clazz = clazz;
            this.index = index;
        }

        public String getClazz() {
            return clazz;
        }

        public int getIndex() {
            return index;
        }
    }
}
