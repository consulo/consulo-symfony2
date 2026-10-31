package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpStringLiteralExpressionReference extends PsiReferenceProvider {

    private final ArrayList<Call> oneOfCall = new ArrayList<>();
    private final Class referenceClass;

    public PhpStringLiteralExpressionReference(Class referenceClass) {
        this.referenceClass = referenceClass;
    }

    public PhpStringLiteralExpressionReference addCall(String className, String methodName) {
        this.oneOfCall.add(new Call(className, methodName, 0));
        return this;
    }

    public PhpStringLiteralExpressionReference addCall(String className, String methodName, int index) {
        this.oneOfCall.add(new Call(className, methodName, index));
        return this;
    }

    @Override
    public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList parameterList)) {
            return new PsiReference[0];
        }

        PsiElement methodReference = parameterList.getContext();
        if (!(methodReference instanceof MethodReference)) {
            return new PsiReference[0];
        }

        for(Call call: this.oneOfCall) {
            if (PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, call.getClassName(), call.getMethodName()) && PsiElementUtils.getParameterIndexValue(psiElement) == call.getIndex()) {
                return this.getPsiReferenceBase(psiElement);
            }
        }

        return new PsiReference[0];
    }

    @SuppressWarnings("unchecked")
    private PsiReference[] getPsiReferenceBase(PsiElement psiElement) {

        try {
            PsiReferenceBase referenceClassInstance = (PsiReferenceBase) this.referenceClass.getDeclaredConstructor(StringLiteralExpression.class).newInstance(psiElement);
            return new PsiReference[]{  referenceClassInstance };
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
        }

        return new PsiReference[0];
    }

    private static class Call {
        private final String className;
        private final String methodName;
        private int index = 0;

        public Call(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public Call(String className, String methodName, int index) {
            this(className, methodName);
            this.index = index;
        }

        private int getIndex() {
            return index;
        }

        private String getClassName() {
            return className;
        }

        private String getMethodName() {
            return methodName;
        }
    }

    public boolean acceptsTarget(PsiElement target) {
        return Symfony2ProjectComponent.isEnabled(target);
    }
}