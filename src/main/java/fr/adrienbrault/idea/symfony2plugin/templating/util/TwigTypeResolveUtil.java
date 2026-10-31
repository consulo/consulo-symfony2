package fr.adrienbrault.idea.symfony2plugin.templating.util;

import consulo.language.ast.ASTNode;
import consulo.component.extension.ExtensionPointName;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.ast.IElementType;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.StaticVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.ForLoopVariableResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormVarsResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.NestedFormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigTypeResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.captureVariableOrField;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeResolveUtil {
    private static final String DOC_TYPE_ATOM_PATTERN = "[\\w\\\\\\[\\]]+";
    private static final String DOC_UNION_TYPE_PATTERN = DOC_TYPE_ATOM_PATTERN + "(?:\\|" + DOC_TYPE_ATOM_PATTERN + ")*";
    private static final String DOC_TYPE_BOUNDARY_PATTERN = "(?=\\s|#|$)";

    /**
     * {# variable \AppBundle\Entity\Foo[] #}
     */
    public static final String DEPRECATED_DOC_TYPE_PATTERN = "\\{#[\\s]+(?<var>[\\w]+)[\\s]+(?<class>" + DOC_UNION_TYPE_PATTERN + ")[\\s]+#}";

    /**
     * {# @var variable \AppBundle\Entity\Foo[] #}
     * {# @var variable \AppBundle\Entity\Foo #}
     */
    private static final String DOC_TYPE_PATTERN_CLASS_SECOND = "@var\\s+(?<var>\\w+)\\s+(?<class>" + DOC_UNION_TYPE_PATTERN + ")" + DOC_TYPE_BOUNDARY_PATTERN + "\\s*";

    /**
     * {# @var \AppBundle\Entity\Foo[] variable #}
     * {# @var \AppBundle\Entity\Foo variable #}
     */
    private static final String DOC_TYPE_PATTERN_CLASS_FIRST = "@var\\s+(?<class>" + DOC_UNION_TYPE_PATTERN + ")\\s+(?<var>\\w+)" + DOC_TYPE_BOUNDARY_PATTERN + "\\s*";

    /**
     * Matches only Twig {@code @var} docs.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code {# @var variable \AppBundle\Entity\Foo #}}</li>
     *   <li>{@code {# @var \AppBundle\Entity\Foo variable #}}</li>
     * </ul>
     */
    public static final Pattern[] INLINE_VAR_DOC_REGEX = {
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_SECOND, Pattern.MULTILINE),
        Pattern.compile(DOC_TYPE_PATTERN_CLASS_FIRST, Pattern.MULTILINE),
    };

    /**
     * Matches Twig {@code @var} docs and the deprecated inline doc syntax.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code {# @var variable \AppBundle\Entity\Foo #}}</li>
     *   <li>{@code {# @var \AppBundle\Entity\Foo variable #}}</li>
     *   <li>{@code {# variable \AppBundle\Entity\Foo #}}</li>
     * </ul>
     */
    public static final Pattern[] INLINE_DOC_REGEX = {
        INLINE_VAR_DOC_REGEX[0],
        INLINE_VAR_DOC_REGEX[1],
        Pattern.compile(DEPRECATED_DOC_TYPE_PATTERN),
    };

    // for supporting completion and navigation of one line element
    public static final String[] DOC_TYPE_PATTERN_SINGLE  = new String[] {
        "^(?<var>[\\w]+)[\\s]+(?<class>" + DOC_UNION_TYPE_PATTERN + ")" + DOC_TYPE_BOUNDARY_PATTERN + "[\\s]*$",
        DOC_TYPE_PATTERN_CLASS_SECOND,
        DOC_TYPE_PATTERN_CLASS_FIRST
    };

    private static final String[] PROPERTY_SHORTCUTS = new String[] {"get", "is", "has"};

    private static final ExtensionPointName<TwigFileVariableCollector> TWIG_FILE_VARIABLE_COLLECTORS =
        ExtensionPointName.create(TwigFileVariableCollector.class);

    private static final Key<CachedValue<Map<String, String>>> FILE_VARIABLE_DOC_BLOCK_CACHE = new Key<>("TWIG_FILE_VARIABLE_DOC_BLOCK");

    private static final TwigTypeResolver[] TWIG_TYPE_RESOLVERS = new TwigTypeResolver[] {
        new FormVarsResolver(),
        new FormFieldResolver(),
        new NestedFormFieldResolver(),
        new ForLoopVariableResolver(),
    };

    /**
     * Formats a Twig path including the current element.
     *
     * Method calls can appear at any previous segment.
     * Example: {@code root.getChildren().fff('x').bar} => {@code [root, getChildren, fff, bar]}.
     */
    public static Collection<String> formatPsiTypeNameWithCurrent(PsiElement psiElement) {
        return collectPsiTypeNameElementsWithCurrent(psiElement)
            .stream()
            .map(PsiElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Collects PSI path elements including the current element.
     *
     * Examples:
     * <ul>
     *   <li>{@code foo.bar.car} => {@code [foo, bar, car]}</li>
     *   <li>{@code root.getChildren().fff('x').bar} => {@code [root, getChildren, fff, bar]}</li>
     * </ul>
     */
    public static List<PsiElement> collectPsiTypeNameElementsWithCurrent(PsiElement psiElement) {
        if (!isPsiTypeNameElement(psiElement.getNode())) {
            return Collections.emptyList();
        }

        return collectPsiTypeNameElementsEndingAt(psiElement.getNode());
    }

    /**
     * Collects the path before the given element.
     *
     * Example: {@code root.getChildren().foo.<caret>} => {@code [root, getChildren, foo]}.
     */
    private static List<PsiElement> collectPsiTypeNameElementsBefore(PsiElement psiElement) {
        ASTNode previousPathElement = isPsiTypeNameElement(psiElement.getNode())
            ? getPreviousPsiTypeNameElement(psiElement.getNode())
            : getPreviousPsiTypeNameElementBefore(psiElement.getNode());

        return previousPathElement == null ? Collections.emptyList() : collectPsiTypeNameElementsEndingAt(previousPathElement);
    }

    /**
     * Walks a dotted path backwards from one known path element.
     */
    private static List<PsiElement> collectPsiTypeNameElementsEndingAt(ASTNode node) {
        List<PsiElement> pathElements = new ArrayList<>();
        PsiElement current = node.getPsi();

        while (current != null) {
            pathElements.add(0, current);

            ASTNode previousPathElement = getPreviousPsiTypeNameElement(current.getNode());
            if (previousPathElement == null) {
                break;
            }

            current = previousPathElement.getPsi();
        }

        return pathElements;
    }

    /**
     * Checks whether another path segment follows this element.
     *
     * Example: {@code root.getChildren().fff('x').bar} is true for {@code getChildren} and {@code fff}.
     */
    public static boolean hasNextPsiTypeNameElement(PsiElement psiElement) {
        ASTNode next = getNextNonWhitespaceLeaf(psiElement.getNode());
        if (next != null && next.getElementType() == TwigTokenTypes.LBRACE) {
            next = getNextNodeAfterMethodCall(next);
        }

        if (next == null || next.getElementType() != TwigTokenTypes.DOT) {
            return false;
        }

        ASTNode afterDot = getNextNonWhitespaceLeaf(next);
        return afterDot != null && isPsiTypeNameElement(afterDot);
    }

    public static Set<String> splitTwigDocTypes(@Nullable String typeExpression) {
        if (StringUtils.isBlank(typeExpression)) {
            return Collections.emptySet();
        }

        return Arrays.stream(typeExpression.split("\\|"))
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Finds the previous path element before a named path segment.
     */
    @Nullable
    private static ASTNode getPreviousPsiTypeNameElement(ASTNode currentNode) {
        ASTNode dot = getPreviousNonWhitespaceLeaf(currentNode);
        if (dot == null || dot.getElementType() != TwigTokenTypes.DOT) {
            return null;
        }

        return getPsiTypeNameElementBeforeDot(dot);
    }

    /**
     * Finds the previous path element before a non-path caret token.
     */
    @Nullable
    private static ASTNode getPreviousPsiTypeNameElementBefore(ASTNode currentNode) {
        ASTNode previous = getPreviousNonWhitespaceLeaf(currentNode);
        if (previous == null) {
            return null;
        }

        if (previous.getElementType() == TwigTokenTypes.DOT) {
            return getPsiTypeNameElementBeforeDot(previous);
        }

        return toPsiTypeNameElement(previous);
    }

    /**
     * Finds a path element directly before a dot.
     */
    @Nullable
    private static ASTNode getPsiTypeNameElementBeforeDot(ASTNode dot) {
        ASTNode previous = getPreviousNonWhitespaceLeaf(dot);
        if (previous == null) {
            return null;
        }

        return toPsiTypeNameElement(previous);
    }

    /**
     * Converts a name-like or method-call tail node into its path element.
     */
    @Nullable
    private static ASTNode toPsiTypeNameElement(ASTNode node) {
        if (isPsiTypeNameElement(node)) {
            return node;
        }

        if (node.getElementType() == TwigTokenTypes.RBRACE) {
            return getMethodCallNameBeforeLeftBrace(node);
        }

        return null;
    }

    /**
     * Twig uses different PSI element types for names depending on context.
     *
     * Examples:
     * <ul>
     *   <li>{@code foo.bar}: {@code bar} is usually an {@code IDENTIFIER}</li>
     *   <li>{@code {% request.isMethod('GET') %}}: {@code request} can be a {@code TAG_NAME}</li>
     *   <li>{@code {% for x in items %}}: {@code items} can be a {@code VARIABLE_REFERENCE}</li>
     *   <li>{@code foo.bar} in some Twig PSI trees: {@code bar} can be a {@code FIELD_REFERENCE}</li>
     * </ul>
     */
    private static boolean isPsiTypeNameElement(ASTNode node) {
        IElementType elementType = node.getElementType();
        return elementType == TwigTokenTypes.IDENTIFIER ||
            elementType == TwigTokenTypes.TAG_NAME ||
            elementType == TwigElementTypes.VARIABLE_REFERENCE ||
            elementType == TwigElementTypes.FIELD_REFERENCE;
    }

    /**
     * Finds the method name before the matching opening parenthesis.
     */
    @Nullable
    private static ASTNode getMethodCallNameBeforeLeftBrace(ASTNode rightBrace) {
        int depth = 0;
        for (ASTNode current = rightBrace; current != null; current = getPreviousNonWhitespaceLeaf(current)) {
            if (current.getElementType() == TwigTokenTypes.RBRACE) {
                depth++;
            } else if (current.getElementType() == TwigTokenTypes.LBRACE) {
                depth--;
                if (depth == 0) {
                    ASTNode identifier = getPreviousNonWhitespaceLeaf(current);
                    return identifier != null ? toPsiTypeNameElement(identifier) : null;
                }
            }
        }

        return null;
    }

    /**
     * Skips a method-call argument list and returns the following leaf.
     */
    @Nullable
    private static ASTNode getNextNodeAfterMethodCall(ASTNode leftBrace) {
        int depth = 0;
        for (ASTNode current = leftBrace; current != null; current = getNextNonWhitespaceLeaf(current)) {
            if (current.getElementType() == TwigTokenTypes.LBRACE) {
                depth++;
            } else if (current.getElementType() == TwigTokenTypes.RBRACE) {
                depth--;
                if (depth == 0) {
                    return getNextNonWhitespaceLeaf(current);
                }
            }
        }

        return null;
    }

    /**
     * Formats a Twig path before the current element.
     * Method calls can appear anywhere before the current element.
     *
     * Examples:
     * <ul>
     *   <li>{@code foo.bar.car} at {@code car} => {@code [foo, bar]}</li>
     *   <li>{@code root.getChildren().fff('x').bar} at {@code bar} => {@code [root, getChildren, fff]}</li>
     * </ul>
     */
    public static Collection<String> formatPsiTypeName(PsiElement psiElement) {
        return collectPsiTypeNameElementsBefore(psiElement)
            .stream()
            .map(PsiElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Collects all possible variables in given path for last given item of "typeName"
     *
     * @param types Variable path "foo.bar" => ["foo", "bar"]
     * @return types for last item of typeName parameter
     */
    public static Collection<TwigTypeContainer> resolveTwigMethodName(PsiElement psiElement, Collection<String> types) {
        if(types.isEmpty()) {
            return Collections.emptyList();
        }

        String rootType = types.iterator().next();
        RootTypeResolve rootTypeResolve = resolveRootTypeContainers(psiElement, rootType);
        Project project = psiElement.getProject();
        Collection<TwigTypeContainer> rootTypes = rootTypeResolve.containers();
        if (!rootTypeResolve.extensionContext() && (types.size() == 1 || hasFormContext(rootTypes))) {
            attachRootMembers(project, rootTypes, rootType, rootTypeResolve.rootVariables());
        }

        return resolveTwigPath(project, rootTypes, types);
    }

    private static void attachRootMembers(Project project, Collection<TwigTypeContainer> types, String rootName, Collection<PsiVariable> variables) {
        for (TwigTypeResolver resolver : TWIG_TYPE_RESOLVERS) {
            resolver.resolve(project, types, types, rootName, new ArrayList<>(), variables);
        }
    }

    private static boolean hasFormContext(Collection<TwigTypeContainer> types) {
        return types.stream().anyMatch(type -> type.getFormViewDataHolder() != null || type.getFormFieldDataHolder() != null);
    }

    private static Collection<TwigTypeContainer> resolveTwigPath(Project project, Collection<TwigTypeContainer> rootTypes, Collection<String> path) {
        Collection<TwigTypeContainer> type = rootTypes;
        Collection<List<TwigTypeContainer>> previousElements = new ArrayList<>();
        previousElements.add(new ArrayList<>(type));

        String[] typeNames = path.toArray(new String[0]);
        for (int i = 1; i <= typeNames.length - 1; i++ ) {
            type = resolveTwigMethodName(project, type, typeNames[i], previousElements);
            previousElements.add(new ArrayList<>(type));

            // we can stop on empty list
            if(type.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return type;
    }

    private static RootTypeResolve resolveRootTypeContainers(PsiElement psiElement, String rootType) {
        // {{ title|u.truncate(20) }} and {% apply u.truncate(20) %}: "u" is provided by a Twig filter.
        if (isTwigFilterChainRoot(psiElement, rootType)) {
            TwigExtension twigExtension = TwigExtensionParser.getFilters(psiElement.getProject()).get(rootType);
            if (twigExtension == null || twigExtension.getTypes().isEmpty()) {
                return new RootTypeResolve(true, Collections.emptyList(), Collections.emptyList());
            }

            return new RootTypeResolve(true, Collections.singletonList(new TwigTypeContainer(twigExtension.getTypes())), Collections.emptyList());
        }

        // {{ ustring('Symfony').truncate(20) }}: "ustring" is provided by a Twig function.
        if (isTwigFunctionChainRoot(psiElement, rootType)) {
            if ("enum".equals(rootType)) {
                return new RootTypeResolve(true, resolveEnumFunctionTypes(psiElement), Collections.emptyList());
            }

            TwigExtension twigExtension = TwigExtensionParser.getFunctions(psiElement.getProject()).get(rootType);
            if (twigExtension == null || twigExtension.getTypes().isEmpty()) {
                return new RootTypeResolve(true, Collections.emptyList(), Collections.emptyList());
            }

            return new RootTypeResolve(true, Collections.singletonList(new TwigTypeContainer(twigExtension.getTypes())), Collections.emptyList());
        }

        Collection<PsiVariable> rootVariables = getRootVariableByName(psiElement, rootType);
        return new RootTypeResolve(false, TwigTypeContainer.fromCollection(rootVariables), rootVariables);
    }

    private record RootTypeResolve(boolean extensionContext, Collection<TwigTypeContainer> containers, Collection<PsiVariable> rootVariables) {
    }

    /**
     * Resolves {@code enum('App\\Status')} from its literal argument, independently of Twig's PHP return declaration.
     */
    private static Collection<TwigTypeContainer> resolveEnumFunctionTypes(PsiElement psiElement) {
        PsiElement root = findRootPathElement(psiElement, "enum");
        if (root == null) {
            return Collections.emptyList();
        }

        PsiElement argument = PsiElementUtils.getNextSiblingAndSkip(root, TwigTokenTypes.STRING_TEXT,
            TwigTokenTypes.LBRACE, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE);
        if (argument == null || PsiElementUtils.getNextSiblingAndSkip(argument, TwigTokenTypes.RBRACE,
            TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE) == null) {
            return Collections.emptyList();
        }

        return TwigConstantEnumResolver.getEnumTargets(argument).stream()
            .map(phpClass -> new TwigTypeContainer(Collections.singleton(phpClass.getFQN())))
            .collect(Collectors.toList());
    }

    /**
     * Detects chains where the first path segment is a filter result.
     *
     * Examples:
     * <ul>
     *   <li>{@code {{ title|u.truncate(20) }}}: {@code u} is the filter root</li>
     *   <li>{@code {% apply u.truncate(20) %}}: {@code u} is the filter root</li>
     * </ul>
     */
    private static boolean isTwigFilterChainRoot(PsiElement psiElement, String rootType) {
        PsiElement rootElement = findRootPathElement(psiElement, rootType);
        if (rootElement == null || rootElement.getNode() == null) {
            return false;
        }

        ASTNode previous = getPreviousNonWhitespaceLeaf(rootElement.getNode());
        if (previous == null) {
            return false;
        }

        if (previous.getElementType() == TwigTokenTypes.FILTER) {
            return true;
        }

        return previous.getElementType() == TwigTokenTypes.TAG_NAME && "apply".equalsIgnoreCase(previous.getText());
    }

    /**
     * Detects chains where the first path segment is a Twig function return value.
     *
     * Example: {@code {{ ustring().truncate(20) }}}.
     */
    private static boolean isTwigFunctionChainRoot(PsiElement psiElement, String rootType) {
        PsiElement rootElement = findRootPathElement(psiElement, rootType);
        if (rootElement == null || rootElement.getNode() == null) {
            return false;
        }

        ASTNode next = getNextNonWhitespaceLeaf(rootElement.getNode());
        if (next == null || next.getElementType() != TwigTokenTypes.LBRACE) {
            return false;
        }

        ASTNode afterCall = getNextNodeAfterMethodCall(next);
        return afterCall != null && afterCall.getElementType() == TwigTokenTypes.DOT;
    }

    @Nullable
    private static PsiElement findRootPathElement(PsiElement psiElement, String rootType) {
        List<PsiElement> pathElements = collectPsiTypeNameElementsWithCurrent(psiElement);
        if (pathElements.isEmpty()) {
            pathElements = collectPsiTypeNameElementsBefore(psiElement);
        }

        if (pathElements.isEmpty()) {
            return null;
        }

        PsiElement rootElement = pathElements.get(0);
        return rootType.equals(rootElement.getText()) ? rootElement : null;
    }

    /**
     * Find scope related inline @var docs
     *
     * "@var foo \Foo"
     */
    private static Map<String, String> findInlineStatementVariableDocBlock(PsiElement psiInsideBlock, IElementType parentStatement, boolean nextParent) {
        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiInsideBlock, psiElement ->
            PlatformPatterns.psiElement(parentStatement).accepts(psiElement)
        );

        Map<String, String> variables = new HashMap<>();
        if(twigCompositeElement == null) {
            return variables;
        }

        Map<String, String> inlineCommentDocsVars = new HashMap<>() {{
            putAll(getInlineCommentDocsVars(twigCompositeElement));
            putAll(getTypesTagVars(twigCompositeElement));
        }};

        // visit parent elements for extending scope
        if(nextParent) {
            PsiElement parent = twigCompositeElement.getParent();
            if(parent != null) {
                inlineCommentDocsVars.putAll(findInlineStatementVariableDocBlock(twigCompositeElement.getParent(), parentStatement, true));
            }
        }

        return inlineCommentDocsVars;
    }

    /**
     * Find file related doc blocks or "types" tags:
     *
     * - "@var foo \Foo"
     * - "{% types {...} %}"
     */
    public static Map<String, String> findFileVariableDocBlock(TwigFile twigFile) {
        return new HashMap<>(LanguageCachedValueUtil.getCachedValue(
            twigFile,
            FILE_VARIABLE_DOC_BLOCK_CACHE,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableMap(findFileVariableDocBlockInner(twigFile)),
                twigFile
            )
        ));
    }

    private static Map<String, String> findFileVariableDocBlockInner(TwigFile twigFile) {
        return new HashMap<>() {{
            putAll(getInlineCommentDocsVars(twigFile));
            putAll(getTypesTagVars(twigFile));
        }};
    }

    /**
     * "@var foo \Foo"
     */
    private static Map<String, String> getInlineCommentDocsVars(PsiElement twigCompositeElement) {
        Map<String, String> variables = new HashMap<>();

        for(PsiElement psiComment: YamlHelper.getChildrenFix(twigCompositeElement)) {
            if(!(psiComment instanceof PsiComment)) {
                continue;
            }

            String text = psiComment.getText();
            if(StringUtils.isBlank(text)) {
                continue;
            }

            for (Pattern pattern : INLINE_DOC_REGEX) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    variables.put(matcher.group("var"), matcher.group("class"));
                }
            }
        }

        return variables;
    }

    /**
     * {% types {...} %}
     */
    private static Map<String, String> getTypesTagVars(PsiElement twigFile) {
        Map<String, String> variables = new HashMap<>();

        for (PsiElement psiComment: YamlHelper.getChildrenFix(twigFile)) {
            if (!(psiComment instanceof TwigCompositeElement) || psiComment.getNode().getElementType() != TwigElementTypes.TAG) {
                continue;
            }

            PsiElement firstChild = psiComment.getFirstChild();
            if (firstChild == null) {
                continue;
            }

            PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
            if (tagName == null || !"types".equals(tagName.getText())) {
                continue;
            }

            ASTNode lbraceCurlPsi = getNextNonWhitespaceLeaf(tagName.getNode());
            if (lbraceCurlPsi == null || lbraceCurlPsi.getElementType() != TwigTokenTypes.LBRACE_CURL) {
                continue;
            }

            ASTNode variableNamePsi = getNextNonWhitespaceLeaf(lbraceCurlPsi);
            if (variableNamePsi == null) {
                continue;
            }

            if (variableNamePsi.getElementType() == TwigTokenTypes.IDENTIFIER) {
                String variableName = variableNamePsi.getText();
                if (!variableName.isBlank()) {
                    variables.put(variableName, getTypesTagVarValue(variableNamePsi.getPsi()));
                }
            }

            for (PsiElement commaPsi : PsiElementUtils.getNextSiblingOfTypes(variableNamePsi.getPsi(), PlatformPatterns.psiElement().withElementType(TwigTokenTypes.COMMA))) {
                ASTNode commaPsiNext = getNextNonWhitespaceLeaf(commaPsi.getNode());
                if (commaPsiNext != null && commaPsiNext.getElementType() == TwigTokenTypes.IDENTIFIER) {
                    String variableName = commaPsiNext.getText();
                    if (!variableName.isBlank()) {
                        variables.put(variableName, getTypesTagVarValue(commaPsiNext.getPsi()));
                    }
                }
            }
        }

        return variables;
    }

    /**
     * Find value tarting scope key:
     * - : 'foo'
     * - : "foo"
     */
    @Nullable
    private static String getTypesTagVarValue(PsiElement psiColon) {
        PsiElement filter = PsiElementUtils.getNextSiblingAndSkip(psiColon, TwigTokenTypes.STRING_TEXT, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.COLON, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.QUESTION);
        if (filter == null) {
            return null;
        }

        String type = PsiElementUtils.trimQuote(filter.getText());
        if (type.isBlank()) {
            return null;
        }

        // secure value
        Matcher matcher = Pattern.compile("^(?<class>" + DOC_UNION_TYPE_PATTERN + ")$").matcher(type);
        if (matcher.find()) {
            // unescape: see also for Twig 4: https://github.com/twigphp/Twig/pull/4199
            return matcher.group("class").replace("\\\\", "\\");
        }

        // unknown
        return "\\mixed";
    }

    public static Map<String, PsiVariable> collectScopeVariables(PsiElement psiElement) {
        return collectScopeVariables(psiElement, new HashSet<>());
    }

    public static Map<String, PsiVariable> collectScopeVariables(PsiElement psiElement, Set<VirtualFile> visitedFiles) {
        PsiFile containingFile = psiElement.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (visitedFiles.contains(virtualFile)) {
            return Collections.emptyMap();
        }

        visitedFiles.add(virtualFile);

        Map<String, VariableData> controllerVars = new HashMap<>();

        TwigFileVariableCollectorParameter collectorParameter = new TwigFileVariableCollectorParameter(psiElement, containingFile, visitedFiles);
        for (TwigFileVariableCollector collector: TWIG_FILE_VARIABLE_COLLECTORS.getExtensions()) {
            Map<String, Set<String>> globalVarsScope = new HashMap<>();
            collector.collect(collectorParameter, globalVarsScope);

            // @TODO: resolve this in change extension point, so that its only possible to provide data and dont give full scope to break / overwrite other variables
            globalVarsScope.forEach((s, strings) -> controllerVars.merge(s, VariableData.fromTypes(strings), VariableData::merge));

            // merging elements
            Map<String, PsiVariable> controllerVars1 = new HashMap<>();
            collector.collectPsiVariables(collectorParameter, controllerVars1);

            controllerVars1.forEach((s, psiVariable) -> controllerVars.merge(s, VariableData.fromPsiVariable(psiVariable), VariableData::merge));
        }

        // globals first @var first
        Collection<Map<String, String>> vars = Arrays.asList(
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.BLOCK_STATEMENT, true),
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.MACRO_STATEMENT, false),
            findInlineStatementVariableDocBlock(psiElement, TwigElementTypes.FOR_STATEMENT, false)
        );

        // Inline Twig docs only provide type strings, e.g. "{# @var form \Symfony\Component\Form\FormView #}".
        for (Map<String, String> entry : vars) {
            entry.forEach((s, s2) -> controllerVars.merge(s, VariableData.fromTypes(splitTwigDocTypes(s2)), VariableData::merge));
        }

        // collect iterator
        controllerVars.replaceAll((s, psiVariable) -> psiVariable.withTypes(collectIteratorReturns(psiElement, psiVariable.types())));

        // check if we are in "for" scope and resolve types ending with []
        collectForArrayScopeVariables(psiElement, controllerVars);

        Map<String, PsiVariable> result = new HashMap<>();
        controllerVars.forEach((s, variableData) -> result.put(s, variableData.toPsiVariable()));

        return result;
    }

    /**
     * Extract magic iterator implementation like "getIterator" or "__iterator"
     *
     * "@TODO find core stuff for resolve possible class return values"
     *
     * "getIterator", "@method Foo __iterator", "@method Foo[] __iterator"
     */
    private static Set<String> collectIteratorReturns(PsiElement psiElement, Set<String> types) {
        Set<String> arrayValues = new HashSet<>();
        for (String type : types) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), type);

            if(phpClass == null) {
                continue;
            }

            for (String methodName : new String[]{"getIterator", "__iterator", "current"}) {
                Method method = phpClass.findMethodByName(methodName);
                if(method != null) {
                    // @method Foo __iterator
                    // @method Foo[] __iterator
                    Set<String> iteratorTypes = method.getType().getTypes();
                    if("__iterator".equals(methodName) || "current".equals(methodName)) {
                        arrayValues.addAll(iteratorTypes.stream().map(x ->
                            !x.endsWith("[]") ? x + "[]" : x
                        ).collect(Collectors.toSet()));
                    } else {
                        // Foobar[]
                        for (String iteratorType : iteratorTypes) {
                            if(iteratorType.endsWith("[]")) {
                                arrayValues.add(iteratorType);
                            }
                        }
                    }
                }
            }
        }

        return arrayValues;
    }

    private static Collection<String> collectForArrayScopeVariablesFoo(Project project, Collection<String> typeName, VariableData variableData) {
        Collection<String> previousElements = variableData.types();

        String[] strings = typeName.toArray(new String[0]);

        for (int i = 1; i <= strings.length - 1; i++ ) {
            previousElements = resolveTwigMethodName(project, previousElements, strings[i]);

            // we can stop on empty list
            if(previousElements.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return previousElements;
    }

    private static void collectForArrayScopeVariables(PsiElement psiElement, Map<String, VariableData> globalVars) {
        PsiElement twigCompositeElement = PsiTreeUtil.findFirstParent(psiElement, psiElement1 -> {
            if (psiElement1 instanceof TwigCompositeElement) {
                return PlatformPatterns.psiElement(TwigElementTypes.FOR_STATEMENT).accepts(psiElement1);
            }
            return false;
        });

        if(!(twigCompositeElement instanceof TwigCompositeElement)) {
            return;
        }

        // Resolve outer loops first so nested form collections retain their entry metadata.
        collectForArrayScopeVariables(twigCompositeElement.getParent(), globalVars);
        globalVars.merge("loop", VariableData.fromType(ForLoopVariableResolver.LOOP_VARIABLE_TYPE), VariableData::merge);

        PsiElement forTag = twigCompositeElement.getFirstChild();
        Pair<String, List<String>> forTagScope = getForTagScope(forTag);
        if (forTagScope == null) {
            return;
        }

        PhpType phpType = new PhpType();
        List<String> forTagInIdentifierString = forTagScope.getSecond();

        VariableData root = globalVars.get(forTagInIdentifierString.getFirst());
        if (root != null && (!root.formTypeFqns().isEmpty() || !root.formFields().isEmpty())) {
            Set<FormFieldDataHolder> entries = collectFormEntries(psiElement.getProject(), root, forTagInIdentifierString);
            if (!entries.isEmpty()) {
                globalVars.merge(forTagScope.getFirst(), VariableData.fromFormFields(entries), VariableData::merge);
                return;
            }
        }

        // {% for coolBar in coolBars.foos %}
        if (forTagInIdentifierString.size() > 1) {
            // nested resolve
            String rootElement = forTagInIdentifierString.getFirst();
            if(globalVars.containsKey(rootElement)) {
                VariableData variableData = globalVars.get(rootElement);
                for (String arrayType : collectForArrayScopeVariablesFoo(psiElement.getProject(), forTagInIdentifierString, variableData)) {
                    phpType.add(arrayType);
                }
            }

        } else {
            String variableName = forTagInIdentifierString.getFirst();
            if(!globalVars.containsKey(variableName)) {
                return;
            }

            // add single "for" var
            for (String s : globalVars.get(variableName).types()) {
                phpType.add(s);
            }
        }

        // find array types; since they are phptypes they ends with []
        Set<String> types = new HashSet<>();
        for(String arrayType: PhpIndex.getInstance(psiElement.getProject()).completeType(psiElement.getProject(), phpType, new HashSet<>()).getTypes()) {
            if(arrayType.endsWith("[]")) {
                types.add(arrayType.substring(0, arrayType.length() -2));
            }
        }

        // we already have same variable in scope, so merge types
        String scopeVariable = forTagScope.getFirst();
        globalVars.merge(scopeVariable, VariableData.fromTypes(types), VariableData::merge);
    }

    private static Set<FormFieldDataHolder> collectFormEntries(Project project, VariableData root, List<String> path) {
        List<PsiVariable> variables = List.of(root.toPsiVariable());
        Collection<TwigTypeContainer> types = TwigTypeContainer.fromCollection(variables);
        attachRootMembers(project, types, path.getFirst(), variables);
        Set<FormFieldDataHolder> entries = new LinkedHashSet<>();
        for (TwigTypeContainer type : resolveTwigPath(project, types, path)) {
            FormFieldDataHolder field = type.getFormFieldDataHolder();
            if (field == null) {
                continue;
            }
            if (type.getStringElement() != null) {
                entries.add(field);
            }
            else {
                FormFieldDataHolder entry = NestedFormFieldResolver.getCollectionEntry(field);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private record VariableData(Set<String> types, Set<String> formTypeFqns, Set<FormFieldDataHolder> formFields) {
        private VariableData {
            types = Set.copyOf(types);
            formTypeFqns = Set.copyOf(formTypeFqns);
            formFields = Set.copyOf(formFields);
        }

        private static VariableData fromPsiVariable(PsiVariable psiVariable) {
            return new VariableData(psiVariable.getTypes(), psiVariable.getFormTypeFqns(), psiVariable.getFormFields());
        }

        private static VariableData fromTypes(Collection<String> types) {
            return new VariableData(new HashSet<>(types), Collections.emptySet(), Collections.emptySet());
        }

        private static VariableData fromFormFields(Set<FormFieldDataHolder> fields) {
            return new VariableData(Set.of("\\Symfony\\Component\\Form\\FormView"), Collections.emptySet(), fields);
        }

        private static VariableData fromType(String type) {
            return fromTypes(Collections.singleton(type));
        }

        private VariableData withTypes(Collection<String> types) {
            return merge(fromTypes(types));
        }

        private VariableData merge(VariableData variableData) {
            Set<String> types = new HashSet<>(this.types);
            types.addAll(variableData.types);

            Set<String> formTypeFqns = new HashSet<>(this.formTypeFqns);
            formTypeFqns.addAll(variableData.formTypeFqns);

            Set<FormFieldDataHolder> formFields = new HashSet<>(this.formFields);
            formFields.addAll(variableData.formFields);
            return new VariableData(types, formTypeFqns, formFields);
        }

        private PsiVariable toPsiVariable() {
            return new PsiVariable(types, formTypeFqns, formFields);
        }
    }

    private static Collection<PsiVariable> getRootVariableByName(PsiElement psiElement, String variableName) {
        Collection<PsiVariable> phpNamedElements = new ArrayList<>();

        for(Map.Entry<String, PsiVariable> variable : collectScopeVariables(psiElement).entrySet()) {
            if(variable.getKey().equals(variableName)) {
                phpNamedElements.add(variable.getValue());
            }
        }

        return phpNamedElements;
    }

    private static Collection<TwigTypeContainer> resolveTwigMethodName(Project project, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> twigTypeContainer) {

        ArrayList<TwigTypeContainer> phpNamedElements = new ArrayList<>();
        boolean formContext = hasFormContext(previousElement);

        for(TwigTypeContainer phpNamedElement: previousElement) {

            for(PhpNamedElement target : getTwigPhpNameTargets(project, phpNamedElement, typeName)) {
                PhpType phpType = target.getType();

                // @TODO: provide extension
                // custom resolving for Twig here: "app.user" => can also be a general solution just support the "getToken()->getUser()"
                if (target instanceof Method && StaticVariableCollector.isUserMethod((Method) target)) {
                    phpNamedElements.addAll(getApplicationUserImplementations(project));
                }

                Set<String> types = phpType.filterPrimitives().getTypes();
                if (!types.isEmpty()) {
                    phpNamedElements.add(new TwigTypeContainer(types));
                }
            }

            if (!formContext) {
                for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                    twigTypeResolver.resolve(project, phpNamedElements, previousElement, typeName, twigTypeContainer, null);
                }
            }
        }

        if (formContext) {
            for(TwigTypeResolver twigTypeResolver: TWIG_TYPE_RESOLVERS) {
                twigTypeResolver.resolve(project, phpNamedElements, previousElement, typeName, twigTypeContainer, null);
            }
        }
        return phpNamedElements;
    }

    /**
     * Get possible suitable UserInterface implementation from the application scope
     */
    private static Collection<TwigTypeContainer> getApplicationUserImplementations(Project project) {
        return PhpIndexUtil
            .getAllSubclasses(project, "\\Symfony\\Component\\Security\\Core\\User\\UserInterface")
            .stream()
            .filter(phpClass -> !phpClass.isInterface()) // filter out implementation like AdvancedUserInterface
            .map(phpClass -> new TwigTypeContainer(Collections.singleton(phpClass.getFQN())))
            .collect(Collectors.toList());
    }

    private static Set<String> resolveTwigMethodName(Project project, Collection<String> previousElement, String typeName) {

        Set<String> types = new HashSet<>();

        for (PhpClass phpClass : PhpElementsUtil.getClassFromPhpTypeSet(project, new HashSet<>(previousElement))) {
            for(PhpNamedElement target : getTwigPhpNameTargets(phpClass, typeName)) {
                types.addAll(target.getType().getTypes());
            }
        }

        return types;
    }

    /**
     *
     * "phpNamedElement.variableName", "phpNamedElement.getVariableName" will resolve php type eg method
     *
     * @param phpNamedElement php class method or field
     * @param variableName variable name shortcut property possible
     * @return matched php types
     */
    public static Collection<? extends PhpNamedElement> getTwigPhpNameTargets(PhpNamedElement phpNamedElement, String variableName) {

        Collection<PhpNamedElement> targets = new ArrayList<>();
        if(phpNamedElement instanceof PhpClass) {

            for(Method method: ((PhpClass) phpNamedElement).getMethods()) {
                String methodName = method.getName();
                if(method.getModifier().isPublic() && (methodName.equalsIgnoreCase(variableName) || isPropertyShortcutMethodEqual(methodName, variableName))) {
                    targets.add(method);
                }
            }

            for(Field field: ((PhpClass) phpNamedElement).getFields()) {
                String fieldName = field.getName();
                if(field.getModifier().isPublic() && fieldName.equalsIgnoreCase(variableName)) {
                    targets.add(field);
                }
            }

            targets.addAll(((PhpClass) phpNamedElement).getEnumCases().stream()
                .filter(enumCase -> variableName.equals(enumCase.getName()))
                .toList());

        }

        return targets;
    }

    public static Collection<PhpClass> resolveTwigTypeClasses(Project project, TwigTypeContainer twigTypeContainer) {
        if (twigTypeContainer.getTypes().isEmpty()) {
            return Collections.emptyList();
        }

        return PhpElementsUtil.getClassFromPhpTypeSet(project, twigTypeContainer.getTypes());
    }

    public static Collection<? extends PhpNamedElement> getTwigPhpNameTargets(Project project, TwigTypeContainer twigTypeContainer, String variableName) {
        Collection<PhpNamedElement> targets = new ArrayList<>();

        for (PhpClass phpClass : resolveTwigTypeClasses(project, twigTypeContainer)) {
            targets.addAll(getTwigPhpNameTargets(phpClass, variableName));
        }

        return targets;
    }


    public static String getTypeDisplayName(Project project, Set<String> types) {

        Collection<PhpClass> classFromPhpTypeSet = PhpElementsUtil.getClassFromPhpTypeSet(project, types);
        if(!classFromPhpTypeSet.isEmpty()) {
            return classFromPhpTypeSet.stream()
                .map(PhpClass::getPresentableFQN)
                .sorted()
                .collect(Collectors.joining("|"));
        }

        PhpType phpType = new PhpType();
        for (String type : types) {
            phpType.add(type);
        }
        PhpType phpTypeFormatted = PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>());

        if(!phpTypeFormatted.getTypes().isEmpty()) {
            return StringUtils.join(new TreeSet<>(phpTypeFormatted.getTypes()), "|");
        }

        if(!types.isEmpty()) {
            return types.iterator().next();
        }

        return "";

    }

    public static boolean isPropertyShortcutMethod(Method method) {
        return isPropertyShortcutMethod(method.getName());
    }

    /**
     * Checks if a method is accessible as a Twig property/method.
     * Methods are accessible if they are:
     * - public
     * - not starting with "set" (setters are not exposed in Twig)
     * - not starting with "__" (magic methods)
     *
     * @param method The method to check
     * @return true if the method is accessible in Twig templates
     */
    public static boolean isTwigAccessibleMethod(Method method) {
        // Early exit on non-public - avoids getName() call for most methods
        if (!method.getModifier().isPublic()) {
            return false;
        }

        String name = method.getName();
        return !name.startsWith("set") && !name.startsWith("__");
    }

    public static boolean isWeakCollectionLikeClass(PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "ArrayAccess") || PhpElementsUtil.isInstanceOf(phpClass, "Iterator");
    }

    public static boolean isPropertyShortcutMethod(String methodName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            if (methodName.startsWith(shortcut) && methodName.length() > shortcut.length()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPropertyShortcutMethodEqual(String methodName, String variableName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            if (methodName.equalsIgnoreCase(shortcut + variableName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the loop variable and iterable path from a Twig for tag.
     *
     * {% for entry in root.children.entries %}
     * => ("entry", ["root", "children", "entries"])
     */
    @Nullable
    public static Pair<String, List<String>> getForTagScope(@Nullable PsiElement forTag) {
        if (forTag == null || forTag.getNode().getElementType() != TwigElementTypes.FOR_TAG) {
            return null;
        }

        PsiElement forScopeVariable = PsiElementUtils.getChildrenOfType(forTag, TwigPattern.getForTagVariablePattern());
        if (forScopeVariable == null) {
            return null;
        }

        List<String> path = new ArrayList<>(getForTagIdentifierAsString(forTag));
        if (path.isEmpty()) {
            return null;
        }

        return Pair.create(forScopeVariable.getText(), Collections.unmodifiableList(path));
    }

    /**
     * Twig attribute shortcuts
     *
     * getFoo => foo
     * hasFoo => foo
     * isFoo => foo
     */
    public static String getPropertyShortcutMethodName(Method method) {
        return getPropertyShortcutMethodName(method.getName());
    }

    public static String getPropertyShortcutMethodName(String methodName) {
        for (String shortcut: PROPERTY_SHORTCUTS) {
            // strip possible property shortcut and make it lcfirst
            if (methodName.startsWith(shortcut) && methodName.length() > shortcut.length()) {
                methodName = methodName.substring(shortcut.length());
                return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
            }
        }

        return methodName;
    }

    /**
     * Get the "for IN" variable identifier as separated string
     *
     *  {% for car in "cars" %}
     *  {% for car in "cars"|length %}
     *  {% for car in "cars.test" %}
     */
    private static Collection<String> getForTagIdentifierAsString(PsiElement forTag) {
        if(forTag.getNode().getElementType() != TwigElementTypes.FOR_TAG) {
            return Collections.emptyList();
        }

        // getChildren hack
        PsiElement firstChild = forTag.getFirstChild();
        if(firstChild == null) {
            return Collections.emptyList();
        }

        // find IN token
        PsiElement psiIn = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.IN));
        if(psiIn == null) {
            return Collections.emptyList();
        }

        // find next IDENTIFIER, eg skip whitespaces
        PsiElement psiIdentifier = PsiElementUtils.getNextSiblingOfType(psiIn, captureVariableOrField());
        if(psiIdentifier == null) {
            return Collections.emptyList();
        }

        // find non common token type. we only allow: "test.test"
        PsiElement afterInVarPsiElement = PsiElementUtils.getNextSiblingOfType(psiIdentifier, PlatformPatterns.psiElement().andNot(PlatformPatterns.or(
            PlatformPatterns.psiElement((TwigTokenTypes.IDENTIFIER)),
            PlatformPatterns.psiElement((TwigTokenTypes.DOT))
        )));

        if(afterInVarPsiElement == null) {
            return Collections.emptyList();
        }

        return TwigTypeResolveUtil.formatPsiTypeName(afterInVarPsiElement);
    }

    /**
     * The leaf before the node, whitespace skipped: a plain walk of the tree
     */
    private static @Nullable ASTNode getPreviousNonWhitespaceLeaf(@Nullable ASTNode node) {
        if (node == null) {
            return null;
        }

        ASTNode treePrev = node.getTreePrev();
        if (treePrev != null) {
            ASTNode candidate = treePrev;
            while (candidate.getLastChildNode() != null) {
                candidate = candidate.getLastChildNode();
            }

            if (!FormatterUtil.isWhitespaceOrEmpty(candidate)) {
                return candidate;
            }
            else {
                return getPreviousNonWhitespaceLeaf(candidate);
            }
        }

        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null || treeParent.getTreeParent() == null) {
            return null;
        }
        else {
            return getPreviousNonWhitespaceLeaf(treeParent);
        }
    }

    /**
     * The leaf after the node, whitespace skipped: a plain walk of the tree, the counterpart of
     * {@link #getPreviousNonWhitespaceLeaf(ASTNode)}
     */
    // TODO consulo-port: consulo.language.codeStyle.FormatterUtil has no getNextNonWhitespaceLeaf(ASTNode); replace this helper once the platform provides it
    private static @Nullable ASTNode getNextNonWhitespaceLeaf(@Nullable ASTNode node) {
        if (node == null) {
            return null;
        }

        ASTNode treeNext = node.getTreeNext();
        if (treeNext != null) {
            ASTNode candidate = treeNext;
            while (candidate.getFirstChildNode() != null) {
                candidate = candidate.getFirstChildNode();
            }

            if (!FormatterUtil.isWhitespaceOrEmpty(candidate)) {
                return candidate;
            }
            else {
                return getNextNonWhitespaceLeaf(candidate);
            }
        }

        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null || treeParent.getTreeParent() == null) {
            return null;
        }
        else {
            return getNextNonWhitespaceLeaf(treeParent);
        }
    }
}
