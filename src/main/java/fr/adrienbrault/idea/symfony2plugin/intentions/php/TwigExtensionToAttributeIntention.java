package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.PhpCallableMethod;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.document.Document;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import icons.SymfonyIcons;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Intention action to migrate TwigExtension getFilters(), getFunctions(), and getTests()
 * methods to use PHP attributes (#[AsTwigFilter], #[AsTwigFunction], #[AsTwigTest]).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.TwigExtensionToAttributeIntention", fileExtensions = "php", categories = "Symfony")
public class TwigExtensionToAttributeIntention extends PsiElementBaseIntentionAction implements Iconable {
    private static final String ABSTRACT_EXTENSION_FQN = "\\Twig\\Extension\\AbstractExtension";
    private static final String TWIG_FILTER_FQN = "Twig\\Attribute\\AsTwigFilter";
    private static final String TWIG_FUNCTION_FQN = "Twig\\Attribute\\AsTwigFunction";
    private static final String TWIG_TEST_FQN = "Twig\\Attribute\\AsTwigTest";

    @Override
    public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
        return null;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        PhpClass phpClass = getTwigExtensionClass(project, psiElement);
        if (phpClass == null) {
            return;
        }

        List<MethodTransformation> transformations = findTransformations(phpClass);
        if (transformations.isEmpty()) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            for (MethodTransformation transformation : transformations) {
                applyTransformation(project, transformation);
            }

            // After all transformations, check if we can remove the extends clause
            checkAndRemoveExtendsAbstractExtension(project, phpClass);
        });
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>PHP</language>
        if (!psiElement.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PhpClass phpClass = getTwigExtensionClass(project, psiElement);
        if (phpClass == null) {
            return false;
        }

        return hasAnyTransformableMethods(phpClass);
    }

    /**
     * Fast check to determine if the class has any of the target methods with transformable content.
     * For performance, we do a lightweight check for non-empty arrays in isAvailable().
     * The full transformation analysis happens in invoke().
     */
    private boolean hasAnyTransformableMethods(PhpClass phpClass) {
        Method getFiltersMethod = phpClass.findOwnMethodByName("getFilters");
        if (getFiltersMethod != null && hasNonEmptyReturnArray(getFiltersMethod)) {
            return true;
        }

        Method getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions");
        if (getFunctionsMethod != null && hasNonEmptyReturnArray(getFunctionsMethod)) {
            return true;
        }

        Method getTestsMethod = phpClass.findOwnMethodByName("getTests");
        if (getTestsMethod != null && hasNonEmptyReturnArray(getTestsMethod)) {
            return true;
        }

        return false;
    }

    /**
     * Check if a method returns a non-empty array.
     * This is a lightweight check - we just look for NewExpression elements in the return array.
     */
    private boolean hasNonEmptyReturnArray(Method method) {
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(method, GroupStatement.class);
        if (groupStatement == null) {
            return false;
        }

        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression) {
                    // Check if the array has any NewExpression elements
                    return PsiTreeUtil.findChildOfType(returnValue, NewExpression.class) != null;
                }
            }
        }

        return false;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Migrate to TwigExtension attributes");
    }

    @Override
    public Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    private @Nullable PhpClass getTwigExtensionClass(Project project, PsiElement element) {
        PsiElement parent = PhpPsiUtil.getParentByCondition(element, true, PhpClass.INSTANCEOF, null);
        if (!(parent instanceof PhpClass phpClass)) {
            return null;
        }

        if (!PhpElementsUtil.isInstanceOf(phpClass, ABSTRACT_EXTENSION_FQN)) {
            return null;
        }

        return phpClass;
    }

    private List<MethodTransformation> findTransformations(PhpClass phpClass) {
        List<MethodTransformation> transformations = new ArrayList<>();

        // Check for getFilters() method
        Method getFiltersMethod = phpClass.findOwnMethodByName("getFilters");
        if (getFiltersMethod != null) {
            transformations.addAll(extractFilterTransformations(getFiltersMethod, phpClass));
        }

        // Check for getFunctions() method
        Method getFunctionsMethod = phpClass.findOwnMethodByName("getFunctions");
        if (getFunctionsMethod != null) {
            transformations.addAll(extractFunctionTransformations(getFunctionsMethod, phpClass));
        }

        // Check for getTests() method
        Method getTestsMethod = phpClass.findOwnMethodByName("getTests");
        if (getTestsMethod != null) {
            transformations.addAll(extractTestTransformations(getTestsMethod, phpClass));
        }

        return transformations;
    }

    private List<MethodTransformation> extractFilterTransformations(Method getFiltersMethod, PhpClass phpClass) {
        return extractTransformations(getFiltersMethod, phpClass, TWIG_FILTER_FQN, "TwigFilter");
    }

    private List<MethodTransformation> extractFunctionTransformations(Method getFunctionsMethod, PhpClass phpClass) {
        return extractTransformations(getFunctionsMethod, phpClass, TWIG_FUNCTION_FQN, "TwigFunction");
    }

    private List<MethodTransformation> extractTestTransformations(Method getTestsMethod, PhpClass phpClass) {
        return extractTransformations(getTestsMethod, phpClass, TWIG_TEST_FQN, "TwigTest");
    }

    private List<MethodTransformation> extractTransformations(Method getMethod, PhpClass phpClass, String attributeFqn, String twigType) {
        List<MethodTransformation> transformations = new ArrayList<>();

        // Find the method body (GroupStatement)
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement.class);
        if (groupStatement == null) {
            return transformations;
        }

        // Look for array return values inside the method body
        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression arrayCreationExpression) {
                    // Parse the array for TwigFilter/TwigFunction/TwigTest instances
                    extractFromArray(arrayCreationExpression, transformations, twigType, attributeFqn, phpClass);
                }
            }
        }

        return transformations;
    }

    private void extractFromArray(ArrayCreationExpression arrayExpr, List<MethodTransformation> transformations, String twigType, String attributeFqn, PhpClass phpClass) {
        // Find all NewExpression elements in the array (handles both indexed and associative arrays)
        Collection<NewExpression> newExpressions = PsiTreeUtil.findChildrenOfType(arrayExpr, NewExpression.class);

        for (NewExpression newExpr : newExpressions) {
            // Check if this is a TwigFilter/TwigFunction/TwigTest instantiation
            ClassReference classReference = newExpr.getClassReference();
            if (classReference == null) {
                continue;
            }

            String className = classReference.getName();
            if (!twigType.equals(className)) {
                continue;
            }

            // Extract parameters from the NewExpression
            PsiElement[] parameters = newExpr.getParameters();
            if (parameters.length < 2) {
                continue; // Need at least name and callable
            }

            String name = null;
            String methodName = null;
            String options = "";

            // First parameter: name (string literal)
            PsiElement nameParameter = parameters[0];
            if (nameParameter instanceof StringLiteralExpression stringLiteralExpression) {
                name = stringLiteralExpression.getContents();
            }

            // Second parameter: callable array [$this, 'methodName'] or [SomeClass::class, 'methodName'] or first-class callable $this->method(...)
            PsiElement callableParameter = parameters[1];
            if (callableParameter instanceof ArrayCreationExpression) {
                // Check if this is [$this, 'methodName'] - only migrate if it references $this
                Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(callableParameter, Variable.class);
                boolean referencesThis = false;
                for (Variable variable : variables) {
                    if ("this".equals(variable.getName())) {
                        referencesThis = true;
                        break;
                    }
                }
                // Only process if it references $this (not other classes)
                if (referencesThis) {
                    Collection<StringLiteralExpression> stringLiterals = PsiTreeUtil.findChildrenOfType(callableParameter, StringLiteralExpression.class);
                    // The method name is the string literal that's not 'this'
                    for (StringLiteralExpression literal : stringLiterals) {
                        String value = literal.getContents();
                        if (!value.isEmpty() && !"this".equals(value)) {
                            methodName = value;
                            break;
                        }
                    }
                }
            }
            else if (callableParameter instanceof PhpCallableMethod phpCallableMethod) {
                // First-class callable syntax: $this->methodName(...)
                methodName = phpCallableMethod.getName();
            }

            // Third parameter: options array (optional)
            PsiElement optionsParameter = parameters.length > 2 ? parameters[2] : null;
            if (optionsParameter instanceof ArrayCreationExpression) {
                Collection<ArrayHashElement> optionElements = PsiTreeUtil.findChildrenOfType(optionsParameter, ArrayHashElement.class);
                StringBuilder optionsBuilder = new StringBuilder();

                for (ArrayHashElement hashElement : optionElements) {
                    PhpPsiElement keyElement = hashElement.getKey();
                    PhpPsiElement valueElement = hashElement.getValue();

                    if (keyElement instanceof StringLiteralExpression keyLiteral && valueElement != null) {
                        String key = keyLiteral.getContents();
                        String value = valueElement.getText();

                        // Convert snake_case to camelCase for PHP attributes
                        key = StringUtils.camelize(key, true);

                        if (!optionsBuilder.isEmpty()) {
                            optionsBuilder.append(", ");
                        }
                        optionsBuilder.append(key).append(": ").append(value);
                    }
                }

                options = optionsBuilder.toString();
            }

            // Validate that we have the required data
            if (name == null || name.isEmpty() || methodName == null || methodName.isEmpty()) {
                continue;
            }

            // Find the target method in the class
            Method targetMethod = phpClass.findOwnMethodByName(methodName);
            if (targetMethod == null) {
                continue; // Method doesn't exist in the class
            }

            // Find the element to delete from the array
            // The NewExpression is wrapped in PhpPsiElementImpl, which is the array element
            PsiElement elementToDelete = newExpr.getParent();

            transformations.add(
                new MethodTransformation(
                    name,
                    methodName,
                    attributeFqn,
                    options,
                    targetMethod,
                    elementToDelete
                )
            );
        }
    }

    private void applyTransformation(Project project, MethodTransformation transformation) {
        PhpClass phpClass = transformation.myMethod.getContainingClass();
        if (phpClass == null) {
            return;
        }

        // Add the attribute import using PhpElementsUtil
        String importedName = PhpElementsUtil.insertUseIfNecessary(phpClass, transformation.myAttributeFqn);
        if (importedName != null) {
            transformation.myImportedName = importedName;
        }

        // Create and add the attribute to the method
        String attributeText = createAttributeText(transformation);
        addAttributeToMethod(project, transformation.myMethod, attributeText);

        // Remove the corresponding entry from the getMethods/getFilters/getTests array
        removeFromArray(transformation);

        // If the array becomes empty, remove the entire getXXX method
        checkAndRemoveEmptyGetMethod(phpClass, transformation.myAttributeFqn);
    }

    private String createAttributeText(MethodTransformation transformation) {
        String attributeName = transformation.myImportedName != null
            ? transformation.myImportedName
            : transformation.myAttributeFqn.substring(transformation.myAttributeFqn.lastIndexOf("\\") + 1);

        if (transformation.myOptions.isEmpty()) {
            return "#[" + attributeName + "('" + transformation.myName + "')]";
        }
        else {
            return "#[" + attributeName + "('" + transformation.myName + "', " + transformation.myOptions + ")]";
        }
    }

    private void addAttributeToMethod(Project project, Method method, String attributeText) {
        // Insert attribute text directly before the method
        PsiFile file = method.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = method.getTextRange().getStartOffset();
        String fullAttributeText = attributeText + "\n";

        document.insertString(methodStartOffset, fullAttributeText);
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Reformat the added attribute with proper indentation
        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);
    }

    private void removeFromArray(MethodTransformation transformation) {
        PsiElement arrayElement = transformation.myArrayElement;
        if (arrayElement == null) {
            return;
        }

        PsiElement prev = arrayElement.getPrevSibling();
        PsiElement next = arrayElement.getNextSibling();

        // First delete the array element itself
        arrayElement.delete();

        // Then handle comma cleanup
        // After deleting the element, we need to check if there's a dangling comma
        if (prev != null && next != null) {
            // Element was in the middle, check for comma after or before
            if (",".equals(next.getText())) {
                next.delete();
            }
            else if (prev.getText().endsWith(",")) {
                // Check if prev sibling is a comma or ends with comma
                if (",".equals(prev.getText())) {
                    prev.delete();
                }
            }
        }
        else if (prev != null && prev.getText().trim().endsWith(",")) {
            // Last element, remove trailing comma from previous element
            if (",".equals(prev.getText().trim())) {
                prev.delete();
            }
        }
        else if (next != null && ",".equals(next.getText().trim())) {
            // First element, remove leading comma
            next.delete();
        }
    }

    private void checkAndRemoveEmptyGetMethod(PhpClass phpClass, String attributeFqn) {
        String methodName = null;
        if (attributeFqn.equals(TWIG_FILTER_FQN)) {
            methodName = "getFilters";
        }
        else if (attributeFqn.equals(TWIG_FUNCTION_FQN)) {
            methodName = "getFunctions";
        }
        else if (attributeFqn.equals(TWIG_TEST_FQN)) {
            methodName = "getTests";
        }

        if (methodName == null) {
            return;
        }

        Method getMethod = phpClass.findOwnMethodByName(methodName);
        if (getMethod == null) {
            return;
        }

        // Check if the method returns an empty array
        // Need to look inside GroupStatement for the return statement
        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(getMethod, GroupStatement.class);
        if (groupStatement == null) {
            return;
        }

        boolean isEmpty = false;
        for (PsiElement child : groupStatement.getChildren()) {
            if (child instanceof PhpReturn phpReturn) {
                PsiElement returnValue = phpReturn.getArgument();
                if (returnValue instanceof ArrayCreationExpression) {
                    // Check if the array has no NewExpression elements
                    isEmpty = PsiTreeUtil.findChildOfType(returnValue, NewExpression.class) == null;
                }
            }
        }

        if (isEmpty) {
            getMethod.delete();
        }
    }

    private void checkAndRemoveExtendsAbstractExtension(Project project, PhpClass phpClass) {
        // Check if getFilters, getFunctions, and getTests methods are all removed
        if (phpClass.findOwnMethodByName("getFilters") != null ||
            phpClass.findOwnMethodByName("getFunctions") != null ||
            phpClass.findOwnMethodByName("getTests") != null
        ) {
            // Some get methods still exist, don't remove extends
            return;
        }

        // Check if the class extends AbstractExtension
        PhpClass superPhpClass = phpClass.getSuperClass();
        String superClass = superPhpClass != null ? superPhpClass.getFQN() : null;
        if (!ABSTRACT_EXTENSION_FQN.equals(superClass) && !"AbstractExtension".equals(superClass)) {
            // Doesn't extend AbstractExtension, nothing to do
            return;
        }

        // Remove the extends clause using shared utility method
        if (CodeUtil.removeExtendsClause(phpClass, ABSTRACT_EXTENSION_FQN)) {
            // Optimize imports to remove unused AbstractExtension import
            optimizeImports(phpClass.getContainingFile());
        }
    }

    private void optimizeImports(PsiFile file) {
        // consulo-port: LanguageImportStatements.INSTANCE.forFile(file) -> ImportOptimizer.forFile(file)
        Set<ImportOptimizer> optimizers = ImportOptimizer.forFile(file);
        if (!optimizers.isEmpty()) {
            for (ImportOptimizer optimizer : optimizers) {
                if (optimizer.supports(file)) {
                    Runnable runnable = optimizer.processFile(file);
                    runnable.run();
                    break;
                }
            }
        }
    }

    private static final class MethodTransformation {
        private final String myName;
        private final String myMethodName;
        private final String myAttributeFqn;
        private final String myOptions;
        private final Method myMethod;
        private final @Nullable PsiElement myArrayElement;
        private @Nullable String myImportedName;

        private MethodTransformation(
            String name,
            String methodName,
            String attributeFqn,
            String options,
            Method method,
            @Nullable PsiElement arrayElement
        ) {
            myName = name;
            myMethodName = methodName;
            myAttributeFqn = attributeFqn;
            myOptions = options;
            myMethod = method;
            myArrayElement = arrayElement;
        }
    }
}
