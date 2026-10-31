package fr.adrienbrault.idea.symfony2plugin.form.usages;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE;

/**
 * Top-level functions of {@code FormFieldReferencesSearchExecutor.kt}.
 */
public final class FormFieldReferencesSearchExecutorKt {
    private FormFieldReferencesSearchExecutorKt() {
    }

    /**
     * Must be called under a read action. Candidates always come from the data-class index.
     */
    public static boolean processFormFieldUsages(PhpNamedElement target, Predicate<StringLiteralExpression> consumer) {
        if (!FormFieldUsageTargetUtilKt.isSupportedFormTarget(target) || !Symfony2ProjectComponent.isEnabled(target.getProject())) {
            return true;
        }

        PhpClass declaringClass = FormFieldUsageTargetUtilKt.formTargetClass(target);
        if (declaringClass == null) {
            return true;
        }

        Project project = target.getProject();
        PsiManager manager = PsiManager.getInstance(project);
        Set<PsiElement> processed = new HashSet<>();

        /*
         * Finish index access before resolving PSI (which can itself access other indexes).
         */
        Map<VirtualFile, Set<String>> candidates = new LinkedHashMap<>();
        Set<String> dataClassNames = new LinkedHashSet<>();
        dataClassNames.add(declaringClass.getFQN());

        // consulo-port: upstream PhpIndex#processAllSubclasses(fqn, processor) is missing in consulo-php, getAllSubclasses(fqn) is used instead
        for (PhpClass subclass : PhpIndex.getInstance(project).getAllSubclasses(declaringClass.getFQN())) {
            ProgressManager.checkCanceled();
            dataClassNames.add(subclass.getFQN());
        }

        for (String fqn : dataClassNames) {
            ProgressManager.checkCanceled();
            FileBasedIndex.getInstance().processValues(
                FormDataClassStubIndex.KEY, "\\" + StringUtils.stripStart(fqn, "\\"), null,
                (file, names) -> {
                    Set<String> merged = new LinkedHashSet<>(candidates.getOrDefault(file, Collections.emptySet()));
                    merged.addAll(names);
                    candidates.put(file, merged);
                    return true;
                },
                GlobalSearchScope.projectScope(project)
            );
        }

        for (Map.Entry<VirtualFile, Set<String>> candidate : candidates.entrySet()) {
            ProgressManager.checkCanceled();
            Set<String> names = candidate.getValue();
            if (!(manager.findFile(candidate.getKey()) instanceof PhpFile phpFile)) {
                continue;
            }

            for (PhpNamedElement topLevelDef : phpFile.getTopLevelDefs().values()) {
                if (!(topLevelDef instanceof PhpClass form) || !names.contains(form.getFQN())) {
                    continue;
                }
                if (!form.getAttributes(AS_FORM_TYPE_ATTRIBUTE).isEmpty()) {
                    continue;
                }

                Method buildForm = form.findMethodByName("buildForm");
                if (buildForm == null) {
                    continue;
                }

                PhpClass dataClass = FormOptionsUtil.getFormPhpClassFromContext(form.getFirstChild());
                if (dataClass == null) {
                    continue;
                }

                if (!dataClassNames.contains(dataClass.getFQN()) || !hasUnambiguousDataClass(form, dataClass)) {
                    continue;
                }

                Map<Method, Set<String>> roots = rootBuilderParameters(buildForm);
                for (MethodReference call : FormUtil.getFormBuilderTypes(buildForm)) {
                    ProgressManager.checkCanceled();
                    PsiElement[] callParameters = call.getParameters();
                    if (callParameters.length == 0 || !(callParameters[0] instanceof StringLiteralExpression literal)) {
                        continue;
                    }

                    if (!isStaticLiteral(literal) || !hasDefaultMapping(call, literal.getContents())) {
                        continue;
                    }

                    if (!(PsiTreeUtil.getParentOfType(call, Function.class) instanceof Method method)) {
                        continue;
                    }

                    if (!isRootBuilder(call.getClassReference(), method, roots.getOrDefault(method, Collections.emptySet()))) {
                        continue;
                    }

                    if (!FormFieldUsageTargetUtilKt.resolvesToFormTarget(literal, dataClass, target)) {
                        continue;
                    }

                    if (processed.add(literal) && !consumer.test(literal)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * The index may contain multiple defaults for one form; do not silently choose the first.
     */
    private static boolean hasUnambiguousDataClass(PhpClass form, PhpClass dataClass) {
        for (String methodName : FormOptionsUtil.FORM_OPTION_METHODS) {
            Method method = form.findMethodByName(methodName);
            if (method == null) {
                continue;
            }

            for (MethodReference call : PhpElementsUtil.collectMethodReferencesInsideControlFlow(method, "setDefault", "setDefaults")) {
                PsiElement[] callParameters = call.getParameters();
                PsiElement value;
                if ("setDefault".equals(call.getName())) {
                    if (callParameters.length == 0 || !(callParameters[0] instanceof StringLiteralExpression key)) {
                        return false;
                    }

                    if (!"data_class".equals(key.getContents())) {
                        continue;
                    }

                    if (callParameters.length < 2) {
                        return false;
                    }

                    value = callParameters[1];
                }
                else {
                    if (callParameters.length == 0 || !(callParameters[0] instanceof ArrayCreationExpression options)) {
                        return false;
                    }

                    value = PhpElementsUtil.getArrayValue(options, "data_class");
                    if (value == null) {
                        continue;
                    }
                }

                String fqn = PhpElementsUtil.getStringValue(value);
                if (fqn == null) {
                    return false;
                }

                if (!PhpLangUtil.equalsClassNames(StringUtils.stripStart(fqn, "\\"), StringUtils.stripStart(dataClass.getFQN(), "\\"))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isStaticLiteral(StringLiteralExpression literal) {
        return PsiTreeUtil.findChildOfType(literal, Variable.class) == null && !literal.getContents().isEmpty();
    }

    private static boolean hasDefaultMapping(MethodReference call, String name) {
        PsiElement[] callParameters = call.getParameters();
        if (callParameters.length < 3) {
            return true;
        }

        if (!(callParameters[2] instanceof ArrayCreationExpression options)) {
            return false;
        }

        for (ArrayHashElement entry : options.getHashElements()) {
            if (!(entry.getKey() instanceof StringLiteralExpression key)) {
                return false;
            }

            if (!isStaticLiteral(key)) {
                return false;
            }

            PhpPsiElement value = entry.getValue();
            switch (key.getContents()) {
                case "mapped" -> {
                    if (value == null || !"true".equalsIgnoreCase(value.getText())) {
                        return false;
                    }
                }
                case "property_path" -> {
                    if (!(value instanceof StringLiteralExpression stringValue) || !isStaticLiteral(stringValue) || !stringValue.getContents().equals(name)) {
                        return false;
                    }
                }
                default -> {
                }
            }
        }

        /*
         * Reject unpacked/dynamic entries too: their options can override mapping.
         */
        for (PsiElement child : options.getChildren()) {
            if (child instanceof PhpPsiElement && !(child instanceof ArrayHashElement)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Mirror FormUtil's one-hop helper traversal, retaining which argument carries the root builder.
     */
    private static Map<Method, Set<String>> rootBuilderParameters(Method buildForm) {
        Set<String> roots = new LinkedHashSet<>();
        for (Parameter parameter : buildForm.getParameters()) {
            boolean isFormBuilder = parameter.getType().getTypes().stream()
                .anyMatch(type -> StringUtils.stripStart(type, "\\").equalsIgnoreCase("Symfony\\Component\\Form\\FormBuilderInterface"));

            if (isFormBuilder) {
                roots.add(parameter.getName());
            }
        }

        Map<Method, Set<String>> result = new LinkedHashMap<>();
        result.put(buildForm, roots);
        Map<Method, Set<String>> rejected = new LinkedHashMap<>();

        for (MethodReference call : PsiTreeUtil.findChildrenOfType(buildForm, MethodReference.class)) {
            ProgressManager.checkCanceled();
            if (!Objects.equals(PsiTreeUtil.getParentOfType(call, Function.class), buildForm)) {
                continue;
            }

            if (!(call.resolve() instanceof Method helper)) {
                continue;
            }

            if (helper.equals(buildForm)) {
                continue;
            }

            Parameter[] helperParameters = helper.getParameters();
            PsiElement[] callParameters = call.getParameters();
            for (int index = 0; index < helperParameters.length; index++) {
                Parameter parameter = helperParameters[index];
                PsiElement argument = index < callParameters.length ? callParameters[index] : null;
                if (isRootBuilder(argument, buildForm, roots)) {
                    Set<String> merged = new LinkedHashSet<>(result.getOrDefault(helper, Collections.emptySet()));
                    merged.add(parameter.getName());
                    result.put(helper, merged);
                }
                else {
                    rejected.computeIfAbsent(helper, k -> new HashSet<>()).add(parameter.getName());
                }
            }
        }

        Map<Method, Set<String>> mapped = new LinkedHashMap<>();
        for (Map.Entry<Method, Set<String>> entry : result.entrySet()) {
            Set<String> names = new LinkedHashSet<>(entry.getValue());
            names.removeAll(rejected.getOrDefault(entry.getKey(), Collections.emptySet()));
            mapped.put(entry.getKey(), names);
        }

        return mapped;
    }

    private static boolean isRootBuilder(@Nullable PsiElement expression, Method method, Set<String> parameters) {
        /*
         * add() returns the same builder; create()/get() return a child builder.
         */
        if (expression instanceof MethodReference methodReference) {
            return "add".equals(methodReference.getName()) && isRootBuilder(methodReference.getClassReference(), method, parameters);
        }

        if (!(expression instanceof Variable variable) || !parameters.contains(variable.getName())) {
            return false;
        }

        /*
         * Be conservative when a parameter has been reassigned, even on another control-flow branch.
         */
        for (AssignmentExpression assignment : PsiTreeUtil.findChildrenOfType(method, AssignmentExpression.class)) {
            if (assignment.getVariable() instanceof Variable assignedVariable && Objects.equals(assignedVariable.getName(), variable.getName())) {
                return false;
            }
        }

        return true;
    }
}
