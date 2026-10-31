package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpDocTagAliasTypeProvider;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.shapes.PhpDocArrayShapeProvider;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpAnyExpressionArgument;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpArrayShapeRegistry;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpShapeEntriesIndex;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Reads PHPDoc array shapes of function parameters, e.g. <code>@param array{size?: 'sm'|'md'} $options</code>.
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public final class PhpDocArrayShapeUtil {
    private static final Key<CachedValue<List<ParameterShape>>> PARAMETER_SHAPES = new Key<>("SYMFONY_PARAMETER_ARRAY_SHAPES");
    private static final int MAX_DEPTH = 10;
    private static final Pattern ALIAS_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private PhpDocArrayShapeUtil() {
    }

    /**
     * Keys of a parameter shape below the given key path, e.g. {@code name} for the path {@code [icon]} of
     * <code>array{icon?: array{name?: string}}</code>.
     */
    public static Collection<ShapeKey> getParameterKeys(Function function, int parameterIndex, List<String> keyPath) {
        Integer index = getParameterIndex(function, parameterIndex);
        if (index == null) {
            return Collections.emptyList();
        }

        ParameterShape shape = getParameterShapes(function).get(index);
        if (shape.readFromDoc() && !keyPath.isEmpty()) {
            PhpDocTag docTag = function.getParameters()[index].getDocTag();
            return docTag != null ? createDocShapeKeys(getPathValueTypes(docTag, keyPath)) : Collections.emptyList();
        }

        Map<String, ShapeNode> nodes = shape.nodes();
        for (String key : keyPath) {
            ShapeNode node = nodes.get(key);
            if (node == null) {
                return Collections.emptyList();
            }

            nodes = node.children();
        }

        return ContainerUtil.map(nodes.values(), ShapeNode::key);
    }

    /**
     * Literal string values of a shape key, e.g. {@code sm} and {@code md} for <code>size?: 'sm'|'md'|string</code>;
     * value types that are type aliases are resolved to their literals.
     */
    public static Collection<String> getParameterLiteralValues(
        Function function,
        int parameterIndex,
        List<String> keyPath,
        String key
    ) {
        Integer index = getParameterIndex(function, parameterIndex);
        PhpDocTag docTag = index != null ? function.getParameters()[index].getDocTag() : null;
        if (docTag == null) {
            return Collections.emptyList();
        }

        Set<String> literals = new LinkedHashSet<>();
        for (PhpDocType type : getPathValueTypes(docTag, Lists.append(keyPath, key))) {
            collectLiterals(type, literals);
        }

        return literals;
    }

    private static List<ParameterShape> getParameterShapes(Function function) {
        return LanguageCachedValueUtil.getCachedValue(function, PARAMETER_SHAPES, () -> {
            PsiModificationTracker tracker = PsiModificationTracker.getInstance(function.getProject());
            return CachedValueProvider.Result.create(createParameterShapes(function), tracker.forLanguage(PhpLanguage.INSTANCE));
        });
    }

    private static List<ParameterShape> createParameterShapes(Function function) {
        Parameter[] parameters = function.getParameters();

        List<ParameterShape> shapes = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            List<PhpShapeEntriesIndex.PhpShapeEntry> entries = PhpArrayShapeRegistry.INSTANCE.getParameterValues(function, i);
            if (!entries.isEmpty()) {
                shapes.add(new ParameterShape(createShapeNodes(entries), false));
                continue;
            }

            // PhpStorm follows an imported type alias only when both classes share a file
            PhpDocTag docTag = parameters[i].getDocTag();
            List<ShapeKey> keys = docTag != null ? createDocShapeKeys(getDocTypes(docTag)) : Collections.emptyList();

            Map<String, ShapeNode> nodes = new LinkedHashMap<>();
            for (ShapeKey key : keys) {
                nodes.put(key.name(), new ShapeNode(key, Collections.emptyMap()));
            }

            shapes.add(new ParameterShape(Collections.unmodifiableMap(nodes), true));
        }

        return Collections.unmodifiableList(shapes);
    }

    /**
     * Merges the entries of one key; PhpStorm returns one entry per union member of the value type.
     */
    private static Map<String, ShapeNode> createShapeNodes(Collection<PhpShapeEntriesIndex.PhpShapeEntry> entries) {
        Map<String, List<PhpShapeEntriesIndex.PhpShapeEntry>> entriesByKey = new LinkedHashMap<>();
        for (PhpShapeEntriesIndex.PhpShapeEntry entry : entries) {
            if (entry.getKey() instanceof PhpAnyExpressionArgument) {
                continue;
            }

            String name = StringUtil.unquoteString(entry.getKey().getValue());
            if (StringUtils.isNotBlank(name)) {
                entriesByKey.computeIfAbsent(name, k -> new ArrayList<>()).add(entry);
            }
        }

        Map<String, ShapeNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, List<PhpShapeEntriesIndex.PhpShapeEntry>> entry : entriesByKey.entrySet()) {
            List<PhpShapeEntriesIndex.PhpShapeEntry> keyEntries = entry.getValue();
            boolean optional = keyEntries.stream().allMatch(PhpShapeEntriesIndex.PhpShapeEntry::isOptional);

            List<PhpShapeEntriesIndex.PhpShapeEntry> children = keyEntries.stream()
                .flatMap(keyEntry -> keyEntry.getChildren().stream())
                .toList();

            ShapeKey key = new ShapeKey(entry.getKey(), optional);
            nodes.put(entry.getKey(), new ShapeNode(key, createShapeNodes(children)));
        }

        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Reads the keys from the PhpDoc PSI; PhpStorm's own shape parser takes over a second on a shape with hundreds of keys.
     */
    private static List<ShapeKey> createDocShapeKeys(Collection<PhpDocType> types) {
        Map<String, Boolean> optionalByKey = new LinkedHashMap<>();
        for (PhpDocType type : types) {
            PsiElement attributeList = getShapeAttributeList(type, 0);
            if (attributeList == null) {
                continue;
            }

            for (ShapeEntry entry : readShapeEntries(attributeList)) {
                optionalByKey.merge(entry.name(), entry.optional(), Boolean::logicalAnd);
            }
        }

        List<ShapeKey> keys = new ArrayList<>();
        optionalByKey.forEach((name, optional) -> keys.add(new ShapeKey(name, optional)));

        return keys;
    }

    /**
     * Value types at the end of a key path, e.g. the types of {@code position} for the path {@code [icon, position]}.
     */
    private static Collection<PhpDocType> getPathValueTypes(PhpDocTag docTag, List<String> keyPath) {
        Collection<PhpDocType> types = getDocTypes(docTag);
        for (String key : keyPath) {
            types = getShapeValueTypes(types, key);
        }

        return types;
    }

    @Nullable
    private static Integer getParameterIndex(Function function, int parameterIndex) {
        Parameter[] parameters = function.getParameters();
        if (parameterIndex < parameters.length) {
            return parameterIndex;
        }

        boolean isVariadic = parameters.length > 0 && parameters[parameters.length - 1].isVariadic();
        return isVariadic ? parameters.length - 1 : null;
    }

    private static List<PhpDocType> getDocTypes(PsiElement element) {
        List<PhpDocType> types = new ArrayList<>();
        for (PsiElement child : PhpPsiUtil.getChildren(element, PhpDocType.class::isInstance)) {
            types.add((PhpDocType) child);
        }

        return types;
    }

    /**
     * Types of a shape key value, e.g. {@code 'sm'} and {@code SizeValue} for <code>array{size?: 'sm'|SizeValue}</code>.
     */
    private static List<PhpDocType> getShapeValueTypes(Collection<PhpDocType> types, String key) {
        List<PhpDocType> valueTypes = new ArrayList<>();
        for (PhpDocType type : types) {
            PsiElement attributeList = getShapeAttributeList(type, 0);
            if (attributeList == null) {
                continue;
            }

            for (ShapeEntry entry : readShapeEntries(attributeList)) {
                if (key.equals(entry.name())) {
                    valueTypes.addAll(entry.valueTypes());
                }
            }
        }

        return valueTypes;
    }

    /**
     * Finds the <code>{...}</code> of a shape type, directly or through a type alias.
     */
    @Nullable
    private static PsiElement getShapeAttributeList(PhpDocType type, int depth) {
        PsiElement attributeList = PhpPsiUtil.getChildOfType(type, PhpDocElementTypes.phpDocAttributeList);
        if (attributeList != null) {
            boolean isShape = PhpPsiUtil.isOfType(attributeList.getFirstChild(), PhpDocTokenTypes.DOC_LBRACE);
            return isShape ? attributeList : null;
        }

        PhpDocType aliasedType = depth < MAX_DEPTH ? getAliasedType(type) : null;
        return aliasedType != null ? getShapeAttributeList(aliasedType, depth + 1) : null;
    }

    /**
     * Reads the entries of <code>{size?: 'sm'|'md', 'data-id': string}</code>.
     */
    private static List<ShapeEntry> readShapeEntries(PsiElement attributeList) {
        List<ShapeEntry> entries = new ArrayList<>();
        StringBuilder keyText = new StringBuilder();
        List<PhpDocType> valueTypes = new ArrayList<>();

        for (PsiElement child = attributeList.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isShapeEntryEnd(child)) {
                ShapeEntry entry = createShapeEntry(keyText.toString(), valueTypes);
                if (entry != null) {
                    entries.add(entry);
                }

                keyText.setLength(0);
                valueTypes = new ArrayList<>();
            } else if (child instanceof PhpDocType type) {
                valueTypes.add(type);
            } else if (valueTypes.isEmpty() && !isShapeSyntax(child)) {
                keyText.append(child.getText());
            }
        }

        return entries;
    }

    /**
     * Creates an entry from key tokens like {@code size?:} or {@code 'data-id':}; list entries have no key.
     */
    @Nullable
    private static ShapeEntry createShapeEntry(String keyText, List<PhpDocType> valueTypes) {
        String key = StringUtils.removeEnd(keyText.trim(), ":").trim();
        boolean optional = key.endsWith("?");

        String name = StringUtil.unquoteString(StringUtils.removeEnd(key, "?").trim());
        if (name.isEmpty() || valueTypes.isEmpty()) {
            return null;
        }

        return new ShapeEntry(name, optional, valueTypes);
    }

    private static boolean isShapeEntryEnd(PsiElement element) {
        return PhpPsiUtil.isOfType(element, PhpDocTokenTypes.DOC_COMMA) || PhpPsiUtil.isOfType(element, PhpDocTokenTypes.DOC_RBRACE);
    }

    private static boolean isShapeSyntax(PsiElement element) {
        return PhpDocArrayShapeProvider.isIgnoredElementInMultilineDocType(element) ||
            PhpPsiUtil.isOfType(element, PhpDocTokenTypes.DOC_LBRACE) ||
            PhpPsiUtil.isOfType(element, PhpDocTokenTypes.DOC_PIPE);
    }

    private static void collectLiterals(PhpDocType type, Set<String> literals) {
        String text = type.getText();
        if (isQuoted(text)) {
            literals.add(StringUtil.unquoteString(text));
            return;
        }

        PhpDocTag aliasTag = getAliasTag(type);
        if (aliasTag != null) {
            collectAliasLiterals(aliasTag, literals, 0);
        }
    }

    /**
     * Reads the literals of <code>@psalm-type SizeValue = 'sm'|'md'|string</code> from its text: PhpStorm builds no
     * types for a union starting with a literal like {@code '-moz-initial'}.
     */
    private static void collectAliasLiterals(PhpDocTag aliasTag, Set<String> literals, int depth) {
        PhpDocComment docComment = PsiTreeUtil.getParentOfType(aliasTag, PhpDocComment.class);
        if (docComment == null || depth > MAX_DEPTH) {
            return;
        }

        for (String member : splitUnionType(getAliasTypeText(aliasTag))) {
            if (isQuoted(member)) {
                literals.add(StringUtil.unquoteString(member));
                continue;
            }

            if (!ALIAS_NAME.matcher(member).matches() || PhpType.isPrimitiveType(member)) {
                continue;
            }

            Collection<PhpDocTag> localTags = PhpDocTagAliasTypeProvider.resolveLocalTypeFromTypeAlias(member, docComment);
            PhpDocTag localTag = ContainerUtil.getFirstItem(localTags);
            PhpDocTag nestedAliasTag = localTag != null ? followImport(localTag, 0) : null;
            if (nestedAliasTag != null) {
                collectAliasLiterals(nestedAliasTag, literals, depth + 1);
            }
        }
    }

    private static String getAliasTypeText(PhpDocTag aliasTag) {
        PsiElement identifier = PhpDocTagAliasTypeProvider.getAliasIdentifier(aliasTag);
        if (identifier == null) {
            return "";
        }

        int typeStart = identifier.getTextRange().getEndOffset() - aliasTag.getTextRange().getStartOffset();
        String typeText = aliasTag.getText().substring(typeStart).trim();
        typeText = StringUtils.removeStart(typeText, "=");

        return typeText.replaceAll("\\n\\s*\\*", " ").trim();
    }

    /**
     * Splits top-level union members, e.g. {@code 'a'|array<'b'|'c'>|string}.
     */
    private static List<String> splitUnionType(String type) {
        List<String> members = new ArrayList<>();
        int start = 0;
        int depth = 0;
        char quote = 0;

        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (quote != 0) {
                quote = c == quote ? 0 : quote;
                continue;
            }

            switch (c) {
                case '\'', '"' -> quote = c;
                case '<', '{', '(', '[' -> depth++;
                case '>', '}', ')', ']' -> depth--;
                case '|' -> {
                    if (depth == 0) {
                        members.add(type.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                default -> {
                }
            }
        }

        members.add(type.substring(start).trim());

        return members;
    }

    private static boolean isQuoted(String text) {
        if (text.length() < 2) {
            return false;
        }

        char first = text.charAt(0);
        return (first == '\'' || first == '"') && text.charAt(text.length() - 1) == first;
    }

    /**
     * Resolves a type alias to its type, e.g. <code>array{...}</code> for {@code ButtonOptions}.
     */
    @Nullable
    private static PhpDocType getAliasedType(PhpDocType type) {
        PhpDocTag aliasTag = getAliasTag(type);
        return aliasTag != null ? PhpPsiUtil.getChildOfClass(aliasTag, PhpDocType.class) : null;
    }

    /**
     * Resolves a type name to its <code>@psalm-type</code> or <code>@phpstan-type</code> tag, following imports.
     */
    @Nullable
    private static PhpDocTag getAliasTag(PhpDocType type) {
        String name = type.getName();
        if (StringUtils.isBlank(name) || PhpType.isPrimitiveType(name)) {
            return null;
        }

        PhpDocTag tag = ContainerUtil.getFirstItem(PhpDocTagAliasTypeProvider.resolveTypeAliases(type));
        return tag != null ? followImport(tag, 0) : null;
    }

    /**
     * Follows <code>@psalm-import-type ButtonOptions from ButtonExtension</code> to the alias declared on the source class.
     */
    @Nullable
    private static PhpDocTag followImport(PhpDocTag tag, int depth) {
        if (!tag.getName().endsWith(PhpDocTagAliasTypeProvider.IMPORT_TYPE_TAG_POSTFIX)) {
            return tag;
        }

        PhpDocType sourceClass = PhpPsiUtil.getChildOfClass(tag, PhpDocType.class);
        String importedName = getImportedAliasName(tag);
        if (sourceClass == null || importedName == null || depth > MAX_DEPTH) {
            return null;
        }

        // getFQN() ignores the "use" statements of the file
        for (ResolveResult result : sourceClass.multiResolve(false)) {
            if (!(result.getElement() instanceof PhpClass phpClass)) {
                continue;
            }

            PhpDocComment docComment = phpClass.getDocComment();
            Collection<PhpDocTag> aliasTags = PhpDocTagAliasTypeProvider.resolveLocalTypeFromTypeAlias(importedName, docComment);
            PhpDocTag aliasTag = ContainerUtil.getFirstItem(aliasTags);
            if (aliasTag != null) {
                return followImport(aliasTag, depth + 1);
            }
        }

        return null;
    }

    /**
     * Name of the imported alias, e.g. {@code ButtonOptions} for <code>@psalm-import-type ButtonOptions from Button as Options</code>.
     */
    @Nullable
    private static String getImportedAliasName(PhpDocTag tag) {
        String previousIdentifier = null;
        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!PhpPsiUtil.isOfType(child, PhpDocTokenTypes.DOC_IDENTIFIER)) {
                continue;
            }

            if ("from".equals(child.getText())) {
                return previousIdentifier;
            }

            previousIdentifier = child.getText();
        }

        return null;
    }

    /**
     * Array shape key, e.g. {@code size} of <code>array{size?: 'sm'|'md'}</code>.
     *
     * @param name unquoted key name
     * @param optional whether the key is declared with {@code ?}
     */
    public record ShapeKey(String name, boolean optional) {
    }

    private record ShapeNode(ShapeKey key, Map<String, ShapeNode> children) {
    }

    /**
     * Keys of a parameter; nested keys of a shape read from the PhpDoc are read on demand, as its aliases can be recursive.
     */
    private record ParameterShape(Map<String, ShapeNode> nodes, boolean readFromDoc) {
    }

    private record ShapeEntry(String name, boolean optional, List<PhpDocType> valueTypes) {
    }
}
