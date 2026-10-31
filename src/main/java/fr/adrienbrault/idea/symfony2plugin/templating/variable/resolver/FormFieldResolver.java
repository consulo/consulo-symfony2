package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldResolver implements TwigTypeResolver {
    public void resolve(Project project, Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (targets.isEmpty() || previousElements == null || !previousElements.isEmpty()) {
            return;
        }

        for (TwigTypeContainer root : List.copyOf(targets)) {
            FormViewDataHolder view = root.getFormViewDataHolder();
            if (view != null) {
                visitFormFields(project, view.formTypeFqns(), field -> targets.add(toTwigTypeContainer(field)));
            }
        }
    }

    public static boolean isFormView(PhpType phpType) {
        return phpType.types()
            .anyMatch(s ->
                s.equals("\\Symfony\\Component\\Form\\FormView") ||
                    s.equals("\\Symfony\\Component\\Form\\FormInterface") // form view is create converting by Symfony on template render
            );
    }

    public static Collection<PhpClass> getFormTypeFromFormFactory(PsiElement formReference) {
        Collection<PhpClass> phpClasses = new ArrayList<>();

        // $form->createView()
        if (formReference instanceof MethodReference) {
            PsiElement form = formReference.getFirstChild();
            if (form instanceof Variable) {
                PsiElement varDecl = ((Variable) form).resolve();
                if (varDecl instanceof Variable) {
                    MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                    if (methodReference != null) {
                        PhpClass phpClass = resolveCall(methodReference);
                        if (phpClass != null) {
                            phpClasses.add(phpClass);
                        }
                    }
                }
            }
        }

        // nested resolve of form view; @TODO: should be some nicer
        // 'foo2' => $form2 => $form2 = $form->createView() => $this->createForm(new Type();
        if (formReference instanceof Variable) {
            PsiElement varDecl = ((Variable) formReference).resolve();


            // 'foo2' => $form2;
            if (varDecl instanceof Variable variable) {
                PhpType type = PhpIndex.getInstance(variable.getProject()).completeType(variable.getProject(), variable.getType(), new HashSet<>());
                if (isFormView(type)) {
                    PsiElement resolve = variable.resolve();
                    if (resolve != null) {
                        MethodReference nextSiblingOfType = PsiTreeUtil.getNextSiblingOfType(resolve, MethodReference.class);
                        if (nextSiblingOfType != null) {
                            PhpClass phpClass = resolveCall(nextSiblingOfType);
                            if (phpClass != null) {
                                phpClasses.add(phpClass);
                            }
                        }
                    }
                }

                MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                if (methodReference != null) {
                    PsiElement scopeVar = methodReference.getFirstChild();

                    // $form2 = $form->createView()
                    if (scopeVar instanceof Variable) {
                        PsiElement varDeclParent = ((Variable) scopeVar).resolve();
                        if (varDeclParent instanceof Variable) {
                            // "$form"->createView();
                            PsiElement resolve = ((Variable) varDeclParent).resolve();
                            if (resolve != null) {
                                MethodReference nextSiblingOfType = PsiTreeUtil.getNextSiblingOfType(resolve, MethodReference.class);
                                if (nextSiblingOfType != null) {
                                    PhpClass phpClass = resolveCall(methodReference);
                                    if (phpClass != null) {
                                        phpClasses.add(phpClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return phpClasses;
    }

    /**
     * Resolves form type FQNs for a form reference such as {@code $form->createView()}.
     *
     * <p>This is the primitive counterpart of {@link #getFormTypeFromFormFactory(PsiElement)}. It may still use PSI
     * internally, but it returns only normalized FQN strings with a leading backslash.</p>
     */
    public static Set<String> getFormTypeFqnsFromFormFactory(PsiElement formReference) {
        Set<String> formTypeFqns = new LinkedHashSet<>();

        for (PhpClass phpClass : getFormTypeFromFormFactory(formReference)) {
            formTypeFqns.add(phpClass.getFQN());
        }

        return formTypeFqns;
    }

    @Nullable
    private static PhpClass resolveCall(MethodReference methodReference) {
        int index = -1;

        if (PhpElementsUtil.isMethodReferenceInstanceOf(
            methodReference,
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "create")
        )) {
            index = 0;
        } else if (PhpElementsUtil.isMethodReferenceInstanceOf(
            methodReference,
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamed")
        )) {
            index = 1;
        }

        if (index < 0) {
            return null;
        }

        PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, index);
        if (formType != null) {
            return FormUtil.getFormTypeClassOnParameter(formType);
        }

        return null;
    }

    private static List<TwigFormField> getTwigFormFields(Method method, PhpClass formTypeClass) {
        List<TwigFormField> twigFormFields = new ArrayList<>();

        for(MethodReference methodReference: FormUtil.getFormBuilderTypes(method)) {

            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if (fieldName == null) {
                continue;
            }

            PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
            String fieldTypeFqn = null;

            // find form field type
            if(psiElement != null) {
                fieldTypeFqn = FormUtil.getFormTypeFqnOnParameter(psiElement);
            }

            PsiElement options = PsiElementUtils.getParameterOrNamedArgument(methodReference.getParameters(), "options", 2);
            twigFormFields.add(new TwigFormField(fieldName, fieldTypeFqn, formTypeClass.getFQN(), null, NestedFormFieldResolver.getFieldOptions(options)));
        }

        return twigFormFields;
    }

    /**
     * Search and resolve: "$form->createView()" to its PhpClass which is a form type
     */
    public static void visitFormReferencesFields(PsiElement formReference, Consumer<TwigTypeContainer> consumer) {
        visitFormFields(formReference.getProject(), getFormTypeFqnsFromFormFactory(formReference), field -> consumer.accept(toTwigTypeContainer(field)));
    }

    /**
     * Visit all form fields in given PhpClass which are already a form type
     */
    public static void visitFormReferencesFields(PhpClass phpClass, Consumer<TwigTypeContainer> consumer) {
        visitFormFields(phpClass.getProject(), Collections.singleton(phpClass.getFQN()), field -> consumer.accept(toTwigTypeContainer(field)));
    }

    public static void visitFormFields(Project project, Collection<String> formTypeFqns, Consumer<TwigFormField> consumer) {
        FormUtil.FormTypeCollector collector = null;

        Collection<Method> methods = new HashSet<>();

        for (String formTypeFqn : formTypeFqns) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, formTypeFqn);
            if (phpClass == null) {
                continue;
            }

            if (collector == null) {
                collector = new FormUtil.FormTypeCollector(project).collect();
            }

            FormTypeClass formType = collector.getFormTypesMap().get(StringUtils.stripStart(phpClass.getFQN(), "\\"));
            if (formType != null && formType.getType() == FormTypeClass.Type.ATTRIBUTE) {
                visitAttributeFormFields(phpClass, collector, consumer);
                continue;
            }

            Method method = phpClass.findMethodByName("buildForm");
            if (method != null) {
                methods.add(method);
            }

            for (PhpClass formInHierarchicalPath : FormUtil.getParentAndExtendingTypes(phpClass, collector)) {
                Method buildFormHierarchical = formInHierarchicalPath.findMethodByName("buildForm");
                if (buildFormHierarchical != null) {
                    methods.add(buildFormHierarchical);
                }
            }
        }

        for (Method method : methods) {
            PhpClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                consumeFieldType(containingClass, consumer);
            }
        }
    }

    private static void visitAttributeFormFields(PhpClass phpClass, FormUtil.FormTypeCollector collector, Consumer<TwigFormField> consumer) {
        Map<String, TwigFormField> fields = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        Set<String> formTypes = new LinkedHashSet<>();

        // Collect child fields first so they override fields from attributed parents.
        for (PhpClass current = phpClass; current != null && visited.add(current.getFQN()); current = current.getSuperClass()) {
            FormTypeClass formType = collector.getFormTypesMap().get(StringUtils.stripStart(current.getFQN(), "\\"));
            if (formType == null || formType.getType() != FormTypeClass.Type.ATTRIBUTE) {
                continue;
            }

            formTypes.add(current.getFQN());
            for (TwigFormField field : getAttributeFormFields(current)) {
                fields.putIfAbsent(field.name(), field);
            }
        }

        String coreFormType = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType";
        formTypes.add(coreFormType);
        PhpClass coreClass = collector.getFormTypeToClass(coreFormType);
        if (coreClass != null) {
            consumeFieldType(coreClass, field -> fields.putIfAbsent(field.name(), field));
        }

        for (FormClass extension : FormOptionsUtil.getExtendedTypeClasses(phpClass.getProject(), formTypes.toArray(String[]::new))) {
            consumeFieldType(extension.getPhpClass(), field -> fields.put(field.name(), field));
        }

        for (TwigFormField field : fields.values()) {
            consumer.accept(field);
        }
    }

    /**
     * Returns visible attribute field names used to search for references.
     */
    public static Set<String> getAttributeFieldNames(Field field) {
        if (field.getAttributes(PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE).isEmpty()) {
            return Collections.emptySet();
        }

        PhpClass owner = field.getContainingClass();
        if (owner == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<>();
        if (owner.isTrait()) {
            // Search by the declared names; references verify the property in each consuming form type.
            for (TwigFormField formField : getAttributeFormFields(owner)) {
                if (field.getName().equals(formField.propertyName())) {
                    names.add(formField.name());
                }
            }
            return names;
        }

        FormTypeClass formType = FormUtil.getFormTypeClasses(field.getProject()).get(StringUtils.stripStart(owner.getFQN(), "\\"));
        if (formType == null || formType.getType() != FormTypeClass.Type.ATTRIBUTE) {
            return Collections.emptySet();
        }

        visitFormFields(field.getProject(), Collections.singleton(owner.getFQN()), formField -> {
            if (field.getName().equals(formField.propertyName()) && owner.getFQN().equals(formField.ownerFormTypeFqn())) {
                names.add(formField.name());
            }
        });
        return names;
    }

    private static List<TwigFormField> getAttributeFormFields(PhpClass phpClass) {
        return LanguageCachedValueUtil.getCachedValue(phpClass, () -> {
            List<TwigFormField> fields = new ArrayList<>();
            Map<String, Field> properties = new LinkedHashMap<>();
            collectOwnAndTraitFields(phpClass, properties, new HashSet<>());

            for (Field field : properties.values()) {
                for (PhpAttribute attribute : field.getAttributes(PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE)) {
                    String name = PhpElementsUtil.getStringValue(PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 2, "name"));
                    String type = PhpElementsUtil.getStringValue(PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "type"));
                    fields.add(new TwigFormField(
                        name != null ? name : field.getName(),
                        type != null ? "\\" + StringUtils.stripStart(type, "\\") : null,
                        phpClass.getFQN(),
                        field.getName(),
                        NestedFormFieldResolver.getFieldOptions(PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 1, "options"))
                    ));
                }
            }

            return CachedValueProvider.Result.create(List.copyOf(fields), PsiModificationTracker.getInstance(phpClass.getProject()).forLanguage(PhpLanguage.INSTANCE));
        });
    }

    /**
     * Resolves an attribute field to its declared class or trait property, ignoring virtual properties.
     */
    @Nullable
    public static Field resolveAttributeField(PhpClass phpClass, String propertyName) {
        Field field = phpClass.findFieldByName(propertyName, false);
        if (field == null || isDeclaredProperty(field)) {
            return field;
        }

        Map<String, Field> fields = new LinkedHashMap<>();
        collectOwnAndTraitFields(phpClass, fields, new HashSet<>());
        return fields.get(propertyName);
    }

    private static boolean isDeclaredProperty(Field field) {
        if (field.isConstant()) {
            return false;
        }

        // property inside a field list or a promoted constructor parameter
        return field instanceof Parameter || PsiUtilCore.getElementType(field.getParent()) == PhpElementTypes.CLASS_FIELDS;
    }

    private static void collectOwnAndTraitFields(PhpClass phpClass, Map<String, Field> fields, Set<String> visited) {
        if (!visited.add(phpClass.getFQN())) {
            return;
        }

        // Class declarations take precedence over properties imported from traits.
        for (Field field : phpClass.getOwnFields()) {
            if (isDeclaredProperty(field)) {
                fields.putIfAbsent(field.getName(), field);
            }
        }
        for (PhpClass trait : phpClass.getTraits()) {
            collectOwnAndTraitFields(trait, fields, visited);
        }
    }

    private static TwigTypeContainer toTwigTypeContainer(TwigFormField field) {
        return new TwigTypeContainer(field.name(), new FormFieldDataHolder(field.fieldTypeFqn(), field.ownerFormTypeFqn(), field.propertyName(), field.options()));
    }

    private static void consumeFieldType(PhpClass phpClass, Consumer<TwigFormField> consumer) {
        Method method = phpClass.findMethodByName("buildForm");
        if (method == null) {
            return;
        }

        for (TwigFormField twigFormField : getTwigFormFields(method, phpClass)) {
            consumer.accept(twigFormField);
        }
    }
}
