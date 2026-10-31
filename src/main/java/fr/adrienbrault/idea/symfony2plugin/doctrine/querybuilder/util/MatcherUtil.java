package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MatcherUtil {

    private static final MethodMatcher.CallToSignature[] SELECT_FIELDS = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "orderBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "addOrderBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "set"),
    };

    private static final MethodMatcher.CallToSignature[] SELECT_FIELDS_VARIADIC = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "select"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "addSelect"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "groupBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "addGroupBy"),
    };

    public static MethodMatcher.@Nullable MethodMatchParameter matchPropertyField(PsiElement psiElement) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterAnyMatcher(psiElement)
            .withSignature(SELECT_FIELDS)
            .match();

        if(methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.StringParameterAnyMatcher(psiElement)
                .withSignature(SELECT_FIELDS_VARIADIC)
                .match();
        }

        if(methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 0)
                .withSignature(SELECT_FIELDS)
                .match();
        }

        return methodMatchParameter;
    }

    public static MethodMatcher.@Nullable MethodMatchParameter matchJoin(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 0)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "join")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin")
            .match();
    }


    public static MethodMatcher.@Nullable MethodMatchParameter matchWhere(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "andWhere")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "where")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "orWhere")
            .match();
    }

    private static MethodMatcher.MethodMatchParameter matchJoinCondition(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 3)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "join")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin")
            .match();
    }

    private static MethodMatcher.MethodMatchParameter matchJoinIndexBy(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 4)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "join")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "leftJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "rightJoin")
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "innerJoin")
            .match();
    }

    private static MethodMatcher.MethodMatchParameter matchCreateQueryBuilderIndexBy(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 1)
            .withSignature("\\Doctrine\\ORM\\EntityRepository", "createQueryBuilder")
            .match();
    }

    private static MethodMatcher.MethodMatchParameter matchFromIndexBy(PsiElement psiElement) {
        return new MethodMatcher.StringParameterMatcher(psiElement, 2)
            .withSignature("\\Doctrine\\ORM\\QueryBuilder", "from")
            .match();
    }

    public static MethodMatcher.@Nullable MethodMatchParameter matchField(PsiElement psiElement) {
        MethodMatcher.MethodMatchParameter methodMatchParameter = MatcherUtil.matchPropertyField(psiElement);
        if (methodMatchParameter == null) {
            methodMatchParameter = MatcherUtil.matchFromIndexBy(psiElement);
        }

        if (methodMatchParameter == null) {
            methodMatchParameter = MatcherUtil.matchJoinCondition(psiElement);
        }

        if (methodMatchParameter == null) {
            methodMatchParameter = MatcherUtil.matchJoinIndexBy(psiElement);
        }

        if (methodMatchParameter == null) {
            methodMatchParameter = MatcherUtil.matchCreateQueryBuilderIndexBy(psiElement);
        }

        if (methodMatchParameter == null) {
            methodMatchParameter = MatcherUtil.matchFromIndexBy(psiElement);
        }

        return methodMatchParameter;
    }
}
