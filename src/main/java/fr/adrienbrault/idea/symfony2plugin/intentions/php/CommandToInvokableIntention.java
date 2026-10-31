package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.UnaryExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.document.Document;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.SymfonyIcons;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Intention action to migrate Symfony Command from execute() to __invoke() style (Symfony 7.3+)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.CommandToInvokableIntention", fileExtensions = "php", categories = "Symfony")
public class CommandToInvokableIntention extends PsiElementBaseIntentionAction implements Iconable {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return;
        }

        Method executeMethod = phpClass.findOwnMethodByName("execute");
        if (executeMethod == null) {
            return;
        }

        // Remove "extends Command" FIRST, before other PSI modifications
        // This must be done before replacing methods to avoid PSI invalidation issues
        if (removeCommandExtends(phpClass)) {
            // Remove parent::__construct() calls since we no longer extend Command
            removeParentConstructorCalls(phpClass);
        }

        // Get parameter names from the execute method
        ParameterNames paramNames = extractParameterNames(executeMethod);

        // Collect arguments and options from configure() method
        ConfigureData configureData = extractConfigureData(phpClass);

        // Migrate the execute method to __invoke by renaming and modifying parameters
        migrateExecuteToInvoke(project, executeMethod, configureData, paramNames);

        // Remove or update configure() method
        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod != null && configureData.canRemoveConfigure()) {
            configureMethod.delete();
        }

        // Optimize imports to remove unused import statements
        optimizeImports(phpClass.getContainingFile());
    }

    private boolean removeCommandExtends(PhpClass phpClass) {
        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        // Use shared utility method to remove the extends clause
        return CodeUtil.removeExtendsClause(phpClass, "\\Symfony\\Component\\Console\\Command\\Command");
    }

    /**
     * Removes parent::__construct() calls from the class constructor.
     * Since we're removing "extends Command", any parent constructor calls become invalid.
     */
    private void removeParentConstructorCalls(PhpClass phpClass) {
        // Find the constructor method
        Method constructor = phpClass.getConstructor();
        if (constructor == null) {
            return;
        }

        // Find the method body
        GroupStatement body = PsiTreeUtil.findChildOfType(constructor, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Find all parent::__construct() calls
        Collection<MethodReference> methodReferences = PsiTreeUtil.findChildrenOfType(body, MethodReference.class);
        List<MethodReference> parentConstructorCalls = new ArrayList<>();

        for (MethodReference methodRef : methodReferences) {
            // Check if this is a parent::__construct() call
            if ("__construct".equals(methodRef.getName())) {
                PhpExpression classReference = methodRef.getClassReference();
                if (classReference != null && "parent".equals(classReference.getText())) {
                    parentConstructorCalls.add(methodRef);
                }
            }
        }

        if (parentConstructorCalls.isEmpty()) {
            return;
        }

        // Remove each parent::__construct() call statement
        for (MethodReference parentCall : parentConstructorCalls) {
            // Navigate up to find the statement containing this call
            Statement statement = PsiTreeUtil.getParentOfType(parentCall, Statement.class);
            if (statement != null) {
                statement.delete();
            }
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

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>PHP</language>
        if (!psiElement.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return false;
        }

        // Check if class extends Command
        if (!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        // check if feature exists
        if (!PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Console\\Command\\InvokableCommand")) {
            return false;
        }

        // Check if execute method exists
        Method executeMethod = phpClass.findOwnMethodByName("execute");
        if (executeMethod == null) {
            return false;
        }

        // Check if __invoke doesn't already exist
        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        return invokeMethod == null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Migrate to invokable command");
    }

    @Override
    public Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    private ParameterNames extractParameterNames(Method executeMethod) {
        ParameterNames names = new ParameterNames();

        Parameter[] parameters = executeMethod.getParameters();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();

            // Get the actual type declaration text as written in source
            PsiElement typeDeclaration = parameter.getTypeDeclaration();
            String typeText = typeDeclaration != null ? typeDeclaration.getText() : "";

            // Check the actual types to identify InputInterface and OutputInterface
            for (String type : parameter.getDeclaredType().getTypes()) {
                if (type.equals("\\Symfony\\Component\\Console\\Input\\InputInterface") || type.equals("\\Symfony\\Component\\Console\\Input\\OutputInterface")) {
                    names.myInputParam = paramName;
                    names.myInputType = typeText;
                    break;
                }
            }
        }

        return names;
    }

    private ConfigureData extractConfigureData(PhpClass phpClass) {
        ConfigureData data = new ConfigureData();

        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod == null) {
            return data;
        }

        // Extract arguments from $this->addArgument() calls
        Collection<MethodReference> addArgumentCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addArgument");
        for (MethodReference methodRef : addArgumentCalls) {
            ArgumentInfo argInfo = parseAddArgument(methodRef);
            if (argInfo != null) {
                data.myArguments.add(argInfo);
            }
        }

        // Extract options from $this->addOption() calls
        Collection<MethodReference> addOptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addOption");
        for (MethodReference methodRef : addOptionCalls) {
            OptionInfo optInfo = parseAddOption(methodRef);
            if (optInfo != null) {
                data.myOptions.add(optInfo);
            }
        }

        // Check if configure has other important calls (setDescription, setHelp, etc.)
        Collection<MethodReference> setDescriptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setDescription");
        Collection<MethodReference> setHelpCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setHelp");

        data.myHasOtherConfigureCalls = !setDescriptionCalls.isEmpty() || !setHelpCalls.isEmpty();

        return data;
    }

    private @Nullable String extractMethodParamName(MethodReference methodRef) {
        PsiElement nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 0);
        if (nameParam == null) {
            return null;
        }
        String name = PhpElementsUtil.getStringValue(nameParam);
        return StringUtils.isBlank(name) ? null : name;
    }

    private @Nullable ArgumentInfo parseAddArgument(MethodReference methodRef) {
        // addArgument(name, mode, description, default)
        ArgumentInfo info = new ArgumentInfo();

        info.myName = extractMethodParamName(methodRef);
        if (info.myName == null) {
            return null;
        }

        // Get mode (parameter 1) - InputArgument::REQUIRED, OPTIONAL, IS_ARRAY
        PsiElement modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1);
        if (modeParam != null) {
            String modeStr = modeParam.getText();
            info.myIsOptional = modeStr.contains("OPTIONAL");
            info.myIsArray = modeStr.contains("IS_ARRAY");
        }

        // Get description (parameter 2)
        PsiElement descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2);
        if (descParam != null) {
            info.myDescription = PhpElementsUtil.getStringValue(descParam);
        }

        // Get default value (parameter 3)
        PsiElement defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3);
        if (defaultParam != null) {
            info.myDefaultValue = defaultParam.getText();
            info.myIsOptional = true; // If default value exists, it's optional
        }

        return info;
    }

    private @Nullable OptionInfo parseAddOption(MethodReference methodRef) {
        // addOption(name, shortcut, mode, description, default)
        OptionInfo info = new OptionInfo();

        info.myName = extractMethodParamName(methodRef);
        if (info.myName == null) {
            return null;
        }

        // Get shortcut (parameter 1)
        PsiElement shortcutParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 1);
        if (shortcutParam != null) {
            info.myShortcut = PhpElementsUtil.getStringValue(shortcutParam);
        }

        // Get mode (parameter 2) - InputOption::VALUE_NONE, VALUE_REQUIRED, VALUE_OPTIONAL, VALUE_IS_ARRAY
        PsiElement modeParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 2);
        if (modeParam != null) {
            String modeStr = modeParam.getText();
            info.myIsValueNone = modeStr.contains("VALUE_NONE");
            info.myIsArray = modeStr.contains("VALUE_IS_ARRAY") || modeStr.contains("IS_ARRAY");
            info.myIsRequired = modeStr.contains("VALUE_REQUIRED");
        }

        // Get description (parameter 3)
        PsiElement descParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 3);
        if (descParam != null) {
            info.myDescription = PhpElementsUtil.getStringValue(descParam);
        }

        // Get default value (parameter 4)
        PsiElement defaultParam = PsiElementUtils.getMethodParameterPsiElementAt(methodRef, 4);
        if (defaultParam != null) {
            info.myDefaultValue = defaultParam.getText();
        }

        return info;
    }

    private void migrateExecuteToInvoke(Project project, Method executeMethod, ConfigureData configureData, ParameterNames paramNames) {
        // Check which parameters are actually used
        ParameterUsage usage = analyzeParameterUsage(executeMethod, paramNames);

        // Get the document for text-based manipulation
        PsiFile file = executeMethod.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);

        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

        // Add use statements for Argument and Option attributes if needed
        PhpClass phpClass = executeMethod.getContainingClass();
        if (phpClass != null) {
            // Track which attributes we need
            boolean needsArgument = !configureData.myArguments.isEmpty();
            boolean needsOption = !configureData.myOptions.isEmpty();

            if (needsArgument) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Argument");
            }
            if (needsOption) {
                PhpElementsUtil.insertUseIfNecessary(phpClass, "\\Symfony\\Component\\Console\\Attribute\\Option");
            }

            // Commit and unblock after adding use statements to allow further document modifications
            psiDocManager.doPostponedOperationsAndUnblockDocument(document);
        }

        // Build new parameter list
        List<String> newParameters = buildNewParameterList(usage, paramNames, configureData);

        // Replace the entire method signature (from visibility modifier to the opening brace)
        replaceMethodSignature(executeMethod, newParameters, document);

        // Commit document changes to refresh PSI completely
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Get a fresh reference to the file and class for reformatting
        PsiFile freshFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (freshFile != null) {
            PhpClass freshPhpClass = PsiTreeUtil.findChildOfType(freshFile, PhpClass.class);
            if (freshPhpClass != null) {
                Method invokeMethod = freshPhpClass.findOwnMethodByName("__invoke");
                if (invokeMethod != null) {
                    // Replace $input->getArgument() and $input->getOption() calls with direct variables
                    replaceInputMethodCallsWithVariables(invokeMethod, configureData, paramNames);

                    // Commit after replacements
                    psiDocManager.commitDocument(document);
                    psiDocManager.doPostponedOperationsAndUnblockDocument(document);

                    // Reformat the __invoke method
                    reformatMethod(project, invokeMethod);
                }
            }
        }

        // Final commit
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);
    }

    /**
     * Replaces $input->getArgument('name') and $input->getOption('name') calls with direct variable access.
     * Also handles type casts like (int) $input->getArgument('name').
     * Removes redundant self-assignments like $var = $var.
     */
    private void replaceInputMethodCallsWithVariables(Method invokeMethod, ConfigureData configureData, ParameterNames paramNames) {
        GroupStatement body = PsiTreeUtil.findChildOfType(invokeMethod, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Build a map of argument/option names to their variable names
        Map<String, String> argumentVariableMap = new HashMap<>();
        Map<String, String> optionVariableMap = new HashMap<>();

        for (ArgumentInfo arg : configureData.myArguments) {
            String name = arg.myName;
            String variableName = convertToValidPhpVariableName(name);
            argumentVariableMap.put(name, variableName);
        }

        for (OptionInfo opt : configureData.myOptions) {
            String name = opt.myName;
            String variableName = convertToValidPhpVariableName(name);
            optionVariableMap.put(name, variableName);
        }

        // Find all method calls in the body
        Collection<MethodReference> methodCalls = PsiTreeUtil.findChildrenOfType(body, MethodReference.class);
        List<MethodReference> callsToReplace = new ArrayList<>();

        for (MethodReference methodCall : methodCalls) {
            String methodName = methodCall.getName();
            if (methodName == null) {
                continue;
            }

            // Check if this is $input->getArgument() or $input->getOption() on InputInterface
            if ("getArgument".equals(methodName)) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getArgument")) {
                    callsToReplace.add(methodCall);
                }
            }
            else if ("getOption".equals(methodName)) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf(methodCall, "\\Symfony\\Component\\Console\\Input\\InputInterface", "getOption")) {
                    callsToReplace.add(methodCall);
                }
            }
        }

        // Replace each call
        for (MethodReference methodCall : callsToReplace) {
            String methodName = methodCall.getName();
            if (methodName == null) {
                continue;
            }

            // Get the argument/option name from the first parameter
            PsiElement nameParam = PsiElementUtils.getMethodParameterPsiElementAt(methodCall, 0);
            if (nameParam == null) {
                continue;
            }

            String paramName = PhpElementsUtil.getStringValue(nameParam);
            if (StringUtils.isBlank(paramName)) {
                continue;
            }

            // Get the variable name from our map
            String variableName = "getArgument".equals(methodName)
                ? argumentVariableMap.get(paramName)
                : optionVariableMap.get(paramName);

            if (variableName == null) {
                continue;
            }

            // Check if the method call is wrapped in a type cast
            PsiElement parent = methodCall.getParent();
            PsiElement elementToReplace = methodCall;

            if (parent instanceof UnaryExpression unaryExpression) {
                // Check if this is a cast expression by looking at the operator
                PsiElement operator = unaryExpression.getOperation();
                if (operator != null) {
                    String operatorText = operator.getText();
                    // Common PHP type casts: (int), (string), (bool), (array), (object), (float), (double)
                    if (operatorText.matches("\\(\\s*(int|integer|string|bool|boolean|array|object|float|double|real)\\s*\\)")) {
                        elementToReplace = parent;
                    }
                }
            }

            // Create a new variable reference
            PhpPsiElement newVariable = PhpPsiElementFactory.createFromText(
                invokeMethod.getProject(),
                Variable.class,
                "$" + variableName
            );

            if (newVariable != null) {
                elementToReplace.replace(newVariable);
            }
        }

        // Remove redundant self-assignments like $var = $var
        removeRedundantSelfAssignments(body);
    }

    /**
     * Removes redundant self-assignments like $var = $var.
     * These occur when we replace $input->getArgument('name') with $name,
     * and the original code had $name = $input->getArgument('name').
     */
    private void removeRedundantSelfAssignments(GroupStatement body) {
        Collection<AssignmentExpression> assignments = PsiTreeUtil.findChildrenOfType(body, AssignmentExpression.class);
        List<PsiElement> statementsToRemove = new ArrayList<>();

        for (AssignmentExpression assignment : assignments) {
            PhpPsiElement variable = assignment.getVariable();
            PhpPsiElement value = assignment.getValue();

            // Check if both left and right side are variables
            if (variable instanceof Variable leftVariable && value instanceof Variable rightVariable) {
                String leftVarName = leftVariable.getName();
                String rightVarName = rightVariable.getName();

                // If they're the same variable, mark the statement for removal
                if (Objects.equals(leftVarName, rightVarName)) {
                    // Navigate up to find the statement containing this assignment
                    Statement statement = PsiTreeUtil.getParentOfType(assignment, Statement.class);
                    if (statement != null && !statementsToRemove.contains(statement)) {
                        statementsToRemove.add(statement);
                    }
                }
            }
        }

        // Remove all redundant statements
        for (PsiElement statement : statementsToRemove) {
            statement.delete();
        }
    }

    private void reformatMethod(Project project, Method method) {
        // Reformat only the method signature (parameters), not the entire body
        // Use CodeStyleManager directly to avoid creating extra undo events
        PsiFile containingFile = method.getContainingFile();

        // Find the method body to determine where the signature ends
        GroupStatement body = PsiTreeUtil.findChildOfType(method, GroupStatement.class);
        if (body == null) {
            return;
        }

        // Reformat from the start of the method to the start of the body (just the signature)
        int startOffset = method.getTextRange().getStartOffset();
        int endOffset = body.getTextRange().getStartOffset();

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        try {
            codeStyleManager.reformatRange(containingFile, startOffset, endOffset);
        }
        catch (IncorrectOperationException ignored) {
            // Ignore formatting errors
        }
    }

    private void replaceMethodSignature(Method method, List<String> newParameters, Document document) {
        // Find the method signature in the source
        String methodText = method.getText();
        int methodStartOffset = method.getTextRange().getStartOffset();

        // Find where the method body starts (the opening brace)
        int bodyStartIndex = methodText.indexOf('{');
        if (bodyStartIndex == -1) {
            return;
        }

        // Build the new method signature
        StringBuilder newSignature = new StringBuilder("public function __invoke(");

        if (!newParameters.isEmpty()) {
            newSignature.append("\n");
            newSignature.append(String.join(",\n", newParameters));
            newSignature.append("\n");
        }

        newSignature.append("): int ");

        // Replace everything from the start of the method to the opening brace
        int endOffset = methodStartOffset + bodyStartIndex;
        document.replaceString(methodStartOffset, endOffset, newSignature.toString());
    }

    private List<String> buildNewParameterList(ParameterUsage usage, ParameterNames paramNames, ConfigureData configureData) {
        // Separate required and optional parameters to ensure correct ordering
        List<String> requiredParameters = new ArrayList<>();
        List<String> optionalParameters = new ArrayList<>();

        // InputInterface and OutputInterface are always required parameters
        if (usage.myInputUsed) {
            requiredParameters.add(paramNames.myInputType + " $" + paramNames.myInputParam);
        }

        if (usage.myOutputUsed) {
            requiredParameters.add(paramNames.myOutputType + " $" + paramNames.myOutputParam);
        }

        // Add arguments as parameters (sort by required/optional)
        for (ArgumentInfo arg : configureData.myArguments) {
            String param = buildArgumentParameter(arg);
            if (arg.myIsOptional) {
                optionalParameters.add(param);
            }
            else {
                requiredParameters.add(param);
            }
        }

        // Add options as parameters (all options are optional)
        for (OptionInfo opt : configureData.myOptions) {
            optionalParameters.add(buildOptionParameter(opt));
        }

        // Combine: required parameters first, then optional
        List<String> parameters = new ArrayList<>();
        parameters.addAll(requiredParameters);
        parameters.addAll(optionalParameters);

        return parameters;
    }


    private ParameterUsage analyzeParameterUsage(Method executeMethod, ParameterNames paramNames) {
        ParameterUsage usage = new ParameterUsage();

        GroupStatement groupStatement = PsiTreeUtil.findChildOfType(executeMethod, GroupStatement.class);
        if (groupStatement == null) {
            return usage;
        }

        // Simply check if the input/output variables are used anywhere in the method body
        Collection<Variable> variables = PsiTreeUtil.findChildrenOfType(groupStatement, Variable.class);

        for (Variable variable : variables) {
            String varName = variable.getName();

            if (paramNames.myInputParam.equals(varName)) {
                usage.myInputUsed = true;
            }
            else if (paramNames.myOutputParam.equals(varName)) {
                usage.myOutputUsed = true;
            }
        }

        return usage;
    }

    private String buildArgumentParameter(ArgumentInfo arg) {
        StringBuilder sb = new StringBuilder();

        // Add #[Argument] attribute - use short name, use statement will be added separately
        sb.append("#[Argument(");

        List<String> attributeParams = new ArrayList<>();

        // Convert argument name to valid PHP variable name
        String name = arg.myName;
        String variableName = convertToValidPhpVariableName(name);

        // Add name parameter if the variable name differs from the original argument name
        if (!variableName.equals(name)) {
            attributeParams.add("name: '" + escapeString(name) + "'");
        }

        // Add description if present
        if (StringUtils.isNotBlank(arg.myDescription)) {
            attributeParams.add("description: '" + escapeString(arg.myDescription) + "'");
        }

        sb.append(String.join(", ", attributeParams));
        sb.append(")]");

        // Determine type - use nullable for optional parameters
        String type = arg.myIsArray ? "array" : "string";
        if (arg.myIsOptional) {
            type = "?" + type;
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName);

        // Add default value if optional
        if (arg.myIsOptional) {
            if (StringUtils.isNotBlank(arg.myDefaultValue)) {
                sb.append(" = ").append(arg.myDefaultValue);
            }
            else {
                sb.append(" = null");
            }
        }

        return sb.toString();
    }

    private String buildOptionParameter(OptionInfo opt) {
        StringBuilder sb = new StringBuilder();

        // Add #[Option] attribute - use short name, use statement will be added separately
        sb.append("#[Option(");

        List<String> attributeParams = new ArrayList<>();

        // Convert option name to valid PHP variable name
        String name = opt.myName;
        String variableName = convertToValidPhpVariableName(name);

        // Add name parameter if the variable name differs from the original option name
        if (!variableName.equals(name)) {
            attributeParams.add("name: '" + escapeString(name) + "'");
        }

        // Add shortcut if present
        if (StringUtils.isNotBlank(opt.myShortcut)) {
            attributeParams.add("shortcut: '" + opt.myShortcut + "'");
        }

        // Add description if present
        if (StringUtils.isNotBlank(opt.myDescription)) {
            attributeParams.add("description: '" + escapeString(opt.myDescription) + "'");
        }

        sb.append(String.join(", ", attributeParams));
        sb.append(")]");

        // Determine type based on mode - use nullable for non-VALUE_NONE options
        String type;
        if (opt.myIsValueNone) {
            type = "bool";
        }
        else if (opt.myIsArray) {
            type = "?array";
        }
        else {
            type = "?string";
        }

        // Add parameter
        sb.append(" ").append(type).append(" $").append(variableName);

        // Add default value
        if (StringUtils.isNotBlank(opt.myDefaultValue)) {
            sb.append(" = ").append(opt.myDefaultValue);
        }
        else if (opt.myIsValueNone) {
            sb.append(" = false");
        }
        else {
            // For non-VALUE_NONE options without explicit default, use null
            sb.append(" = null");
        }

        return sb.toString();
    }



    private String escapeString(String str) {
        return str.replace("'", "\\'");
    }

    /**
     * Converts an argument/option name to a valid PHP variable name.
     * PHP variable names must start with a letter or underscore, and can only contain
     * letters, numbers, and underscores.
     *
     * If the name contains invalid characters (like hyphens), it will be converted to camelCase.
     * For example: "user-name" -> "userName", "dry-run" -> "dryRun"
     *
     * @param name The original argument/option name
     * @return A valid PHP variable name
     */
    private String convertToValidPhpVariableName(String name) {
        // Check if the name is already valid
        if (isValidPhpVariableName(name)) {
            return name;
        }

        // Split by hyphens and other non-alphanumeric characters (except underscores)
        String[] parts = Pattern.compile("[^a-zA-Z0-9_]+").split(name);

        if (parts.length == 0) {
            // Fallback if the name is completely invalid
            return "arg";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }

            if (i == 0) {
                // First part: lowercase
                result.append(parts[i].toLowerCase(Locale.getDefault()));
            }
            else {
                // Subsequent parts: capitalize first letter (camelCase)
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase(Locale.getDefault()));
                }
            }
        }

        String finalName = result.toString();

        // Ensure it starts with a letter or underscore
        if (!finalName.isEmpty() && !Character.isLetter(finalName.charAt(0)) && finalName.charAt(0) != '_') {
            finalName = "_" + finalName;
        }

        return finalName.isEmpty() ? "arg" : finalName;
    }

    /**
     * Checks if a string is a valid PHP variable name.
     *
     * @param name The name to check
     * @return true if the name is valid, false otherwise
     */
    private boolean isValidPhpVariableName(String name) {
        if (name.isEmpty()) {
            return false;
        }

        // First character must be a letter or underscore
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }

        // Remaining characters must be letters, numbers, or underscores
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    // Data classes
    private static final class ParameterNames {
        private String myInputParam = "input";
        private String myOutputParam = "output";
        private String myInputType = "InputInterface";
        private String myOutputType = "OutputInterface";
    }

    private static final class ParameterUsage {
        private boolean myInputUsed = false;
        private boolean myOutputUsed = false;
    }

    private static final class ConfigureData {
        private final List<ArgumentInfo> myArguments = new ArrayList<>();
        private final List<OptionInfo> myOptions = new ArrayList<>();
        private boolean myHasOtherConfigureCalls = false;

        private boolean canRemoveConfigure() {
            // Only remove configure if it only contains addArgument/addOption calls
            return !myHasOtherConfigureCalls;
        }
    }

    private static final class ArgumentInfo {
        private @Nullable String myName = null;
        private @Nullable String myDescription = null;
        private @Nullable String myDefaultValue = null;
        private boolean myIsOptional = false;
        private boolean myIsArray = false;
    }

    private static final class OptionInfo {
        private @Nullable String myName = null;
        private @Nullable String myShortcut = null;
        private @Nullable String myDescription = null;
        private @Nullable String myDefaultValue = null;
        private boolean myIsValueNone = false;
        private boolean myIsArray = false;
        private boolean myIsRequired = false;
    }
}
