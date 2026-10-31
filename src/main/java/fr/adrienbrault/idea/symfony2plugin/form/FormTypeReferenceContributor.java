package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormAttributeUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class FormTypeReferenceContributor extends PsiReferenceContributor {
    public static final MethodMatcher.CallToSignature[] BUILDER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "buildForm"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "buildView"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "finishView"),
    };

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        /*
         * #[FormField(options: ['property_path' => 'displayName', 'entry_type' => 'App\\AddressData'])]
         * #[AsFormType(options: ['class' => 'App\\Entity\\Account'])]
         */
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE)),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                    if (!Symfony2ProjectComponent.isEnabled(element)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    ArrayHashElement entry = PsiTreeUtil.getParentOfType(element, ArrayHashElement.class);
                    if (entry == null || !(entry.getParent() instanceof ArrayCreationExpression options)
                        || FormAttributeUtil.getOptionsAttribute(options) == null) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    StringLiteralExpression literal = (StringLiteralExpression) element;
                    String option = PhpElementsUtil.getStringValue(entry.getKey());
                    if ("class".equals(option)) {
                        return new PsiReference[] {new EntityReference(literal, true)};
                    }
                    if ("entry_type".equals(option) || "type".equals(option)) {
                        return new PsiReference[] {new FormTypeReference(literal)};
                    }
                    if ("property_path".equals(option)) {
                        PhpClass dataClass = FormOptionsUtil.getFormPhpClassFromContext(element);
                        if (dataClass != null) {
                            return new PsiReference[] {new FormUnderscoreMethodReference(literal, dataClass)};
                        }
                    }

                    return PsiReference.EMPTY_ARRAY;
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(
                    PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).inside(ParameterList.class)
                ),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
                    if (parameterList == null) {
                        return new PsiReference[0];
                    }

                    PsiElement methodReference = parameterList.getContext();
                    if(!(methodReference instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    if (!PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
                        return new PsiReference[0];
                    }

                    ArrayHashElement arrayHash = PsiTreeUtil.getParentOfType(psiElement, ArrayHashElement.class);
                    if(arrayHash != null && arrayHash.getKey() instanceof StringLiteralExpression key) {
                        ArrayCreationExpression arrayCreation = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);

                        if(arrayCreation == null) {
                            return new PsiReference[0];
                        }

                        // old 3 parameter hold valid array data
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreation);
                        if(currentIndex == null || currentIndex.getIndex() != 2) {
                            return new PsiReference[0];
                        }

                        String keyString = key.getContents();

                        if(keyString.equals("class")) {
                            return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, true)};
                        }
                    }

                    return new PsiReference[0];
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        /*
         * support form type alias references;
         * we dont use completion here, form type resolving depends on container, which is slow stuff
         */
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    // match add('foo', 'type name')
                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                        .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
                        .match();

                    if(methodMatchParameter == null) {
                        methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                            .withSignature(FormUtil.PHP_FORM_NAMED_BUILDER_SIGNATURES)
                            .match();
                    }

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new FormTypeReferenceRef((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // FormBuilderInterface::add('underscore_method')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList parameterList)) {
                        return new PsiReference[0];
                    }

                    PsiElement methodReference = parameterList.getContext();
                    if (!(methodReference instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    if (!PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
                        return new PsiReference[0];
                    }

                    // only use second parameter
                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if (currentIndex == null || currentIndex.getIndex() != 0) {
                        return new PsiReference[0];
                    }

                    PhpClass phpClass = FormOptionsUtil.getFormPhpClassFromContext(psiElement);
                    if(phpClass == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{new FormUnderscoreMethodReference((StringLiteralExpression) psiElement, phpClass)};
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // $form->get('field')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
                        .withSignature("\\Symfony\\Component\\Form\\FormInterface", "get")
                        .withSignature("\\Symfony\\Component\\Form\\FormInterface", "has")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    PhpClass formType = FormUtil.resolveFormGetterCall(methodMatchParameter.getMethodReference());
                    if(formType == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new FormFieldNameReference((StringLiteralExpression) psiElement, formType)
                    };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    private static class FormTypeReferenceRef extends FormTypeReference {
        public FormTypeReferenceRef(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }
}
