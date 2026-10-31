package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;
import consulo.language.editor.WriteCommandAction;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import java.util.function.Consumer;
import consulo.util.lang.ObjectUtil;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ContainerUtil;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.refactoring.PhpNameUtil;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ParameterResolverConsumer;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml.StaticAttributeResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.ParameterVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlHelper {

    static public @Nullable PsiElement getLocalServiceName(PsiFile psiFile, String findServiceName) {
        return new YamlLocalServiceMap().getLocalServiceName(psiFile, findServiceName);
    }

    static public Map<String, String> getLocalParameterMap(PsiFile psiElement) {
        return new YamlLocalServiceMap().getLocalParameterMap(psiElement);
    }

    static public PsiElement getLocalParameterMap(PsiFile psiFile, String parameterName) {
        return new YamlLocalServiceMap().getLocalParameterName(psiFile, parameterName);
    }

    /**
     * getChildren eg on YamlArray is empty, provide workaround
     */
    static public PsiElement[] getChildrenFix(PsiElement psiElement) {
        List<PsiElement> psiElements = new ArrayList<>();

        PsiElement startElement = psiElement.getFirstChild();
        if(startElement == null) {
            return psiElements.toArray(new PsiElement[0]);
        }

        psiElements.add(startElement);

        for (PsiElement child = psiElement.getFirstChild().getNextSibling(); child != null; child = child.getNextSibling()) {
            psiElements.add(child);
        }

        return psiElements.toArray(new PsiElement[0]);
    }

    /**
     *  FOO:
     *    - foobar
     *
     *  FOO: [foobar]
     */
    static public Collection<YAMLSequenceItem> getSequenceItems(YAMLKeyValue yamlKeyValue) {
        PsiElement yamlSequence = yamlKeyValue.getLastChild();

        if(yamlSequence instanceof YAMLSequence) {
            return ((YAMLSequence) yamlSequence).getItems();
        }

        return Collections.emptyList();
    }

    /**
     * [ROLE_USER, FEATURE_ALPHA, ROLE_ALLOWED_TO_SWITCH]
     */
    static public Collection<String> getYamlArrayValuesAsString(YAMLSequence yamlArray) {
        return new HashSet<>(getYamlArrayValuesAsList(yamlArray));
    }

    /**
     * [ROLE_USER, FEATURE_ALPHA, ROLE_ALLOWED_TO_SWITCH]
     */
    static public Collection<String> getYamlArrayValuesAsList(YAMLSequence yamlArray) {
        Collection<String> keys = new ArrayList<>();

        for (YAMLSequenceItem yamlSequenceItem : yamlArray.getItems()) {
            YAMLValue value = yamlSequenceItem.getValue();
            if(!(value instanceof YAMLScalar)) {
                continue;
            }

            String textValue = ((YAMLScalar) value).getTextValue();
            if(StringUtils.isNotBlank(textValue)) {
                keys.add(textValue);
            }
        }

        return keys;
    }

    public static @Nullable YAMLKeyValue getYamlKeyValue(@Nullable PsiElement yamlCompoundValue, String keyName) {
        return getYamlKeyValue(yamlCompoundValue, keyName, false);
    }

    public static @Nullable YAMLKeyValue getYamlKeyValue(@Nullable PsiElement yamlCompoundValue, String keyName, boolean ignoreCase) {
        if (!(yamlCompoundValue instanceof YAMLMapping)) {
            return null;
        }

        if (!ignoreCase) {
            return ((YAMLMapping) yamlCompoundValue).getKeyValueByKey(keyName);
        }
        
        YAMLKeyValue classKeyValue;
        classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns.string().oneOfIgnoreCase(keyName)));

        if(classKeyValue == null) {
            return null;
        }

        return classKeyValue;
    }

    private static class YamlLocalServiceMap {

        public Map<String, String> getLocalParameterMap(PsiFile psiFile) {
            Map<String, String> map = new HashMap<>();

            for(YAMLKeyValue yamlParameterArray: getQualifiedKeyValuesInFile((YAMLFile) psiFile, "parameters")) {
                String keyName = yamlParameterArray.getKeyText();
                if(StringUtils.isBlank(keyName)) {
                    continue;
                }

                // extract parameter value
                String textValue = null;
                PsiElement value = yamlParameterArray.getValue();
                if(value instanceof YAMLScalar) {
                    String myTextValue = ((YAMLScalar) value).getTextValue();
                    if(!myTextValue.isEmpty() && myTextValue.length() < 150) {
                        textValue = myTextValue;
                    }
                }

                map.put(keyName.toLowerCase(), textValue);
            }

            return map;
        }

        public @Nullable PsiElement getLocalServiceName(PsiFile psiFile, String findServiceName) {
            return getYamlKeyPath(psiFile, findServiceName, "services");
        }

        public @Nullable PsiElement getLocalParameterName(PsiFile psiFile, String findServiceName) {
            return getYamlKeyPath(psiFile, findServiceName, "parameters");
        }

        private @Nullable PsiElement getYamlKeyPath(PsiFile psiFile, final String findServiceName, String rootKey) {

            final Collection<PsiElement> psiElements = new ArrayList<>();

            // @TODO: support case insensitive
            visitQualifiedKeyValuesInFile((YAMLFile) psiFile, rootKey, yamlKeyValue -> {
                if(findServiceName.equalsIgnoreCase(yamlKeyValue.getKeyText())) {
                    psiElements.add(yamlKeyValue);
                }
            });

            if(psiElements.isEmpty()) {
                return null;
            }

            // @TODO: provide support for multiple targets
            return psiElements.iterator().next();
        }
    }

    public static boolean isValidParameterName(String parameterName) {

        if(parameterName.length() < 3) {
            return false;
        }

        if(!parameterName.startsWith("%") || !parameterName.endsWith("%") || parameterName.toLowerCase().startsWith("%env(")) {
            return false;
        }

        // use regular expr here?
        // %kernel.root_dir%/../web/%webpath_modelmasks%
        if(parameterName.contains("/") || parameterName.contains("..")) {
            return false;
        }
        
        // more than 2x "%" is invalid
        return !parameterName.substring(1, parameterName.length() - 1).contains("%");

    }

    public static String trimSpecialSyntaxServiceName(String serviceName) {

        if(serviceName.startsWith("@")) {
            serviceName = serviceName.substring(1);
        }

        // yaml strict syntax
        if(serviceName.endsWith("=")) {
            serviceName = serviceName.substring(0, serviceName.length() -1);
        }

        // optional syntax
        if(serviceName.startsWith("?")) {
            serviceName = serviceName.substring(1);
        }

        return serviceName;

    }

    public static @Nullable String getYamlKeyName(YAMLKeyValue yamlKeyValue) {

        PsiElement modelName = yamlKeyValue.getKey();
        if(modelName == null) {
            return null;
        }

        String keyName = StringUtils.trim(modelName.getText());
        if(keyName.endsWith(":")) {
            keyName = StringUtils.trim((keyName.substring(0, keyName.length() - 1)));
        }

        return keyName;
    }

    public static @Nullable Set<String> getKeySet(@Nullable YAMLKeyValue yamlKeyValue) {

        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(yamlCompoundValue == null) {
            return null;
        }

        Set<String> keySet = new HashSet<>();
        for(YAMLKeyValue yamlKey: PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class)) {
            String fieldName = getYamlKeyName(yamlKey);
            if(fieldName != null) {
                keySet.add(fieldName);
            }
        }

        return keySet;
    }

    public static @Nullable YAMLKeyValue getYamlKeyValue(YAMLMapping yamlHash, String keyName) {
        return getYamlKeyValue(yamlHash, keyName, false);
    }

    /**
     * test:
     *   DEBUG_WEB_1: 1
     *   DEBUG_WEB_2: 1
     */
    public static Map<String, YAMLValue> getYamlArrayKeyMap(YAMLMapping yamlHash) {
        Map<String, YAMLValue> keys = new HashMap<>();

        for(YAMLKeyValue yamlKeyValue: PsiTreeUtil.getChildrenOfTypeAsList(yamlHash, YAMLKeyValue.class)) {
            String keyText = yamlKeyValue.getKeyText();
            if (StringUtils.isNotBlank(keyText)) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value != null) {
                    keys.put(keyText, value);
                }
            }
        }

        return keys;
    }

    public static @Nullable String getYamlKeyValueAsString(YAMLMapping yamlHash, String keyName) {
        YAMLKeyValue yamlKeyValue = getYamlKeyValue(yamlHash, keyName, false);
        if(yamlKeyValue == null) {
            return null;
        }

        final String valueText = yamlKeyValue.getValueText();
        if(StringUtils.isBlank(valueText)) {
            return null;
        }

        return valueText;
    }


    public static @Nullable YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, String keyName) {
        return getYamlKeyValue(yamlKeyValue, keyName, false);
    }

    /**
     *  foo:
     *     class: "name"
     */
    public static @Nullable String getYamlKeyValueAsString(YAMLKeyValue yamlKeyValue, String keyName) {
        return getYamlKeyValueAsString(yamlKeyValue, keyName, false);
    }

    /**
     *  foo:
     *     class: "name"
     *
     *  class:
     *   - "name"
     *   - "test"
     */
    public static Collection<String> getYamlKeyValueStringOrArray(YAMLKeyValue yamlKeyValue, String keyName) {
        YAMLKeyValue childrenKeyValue = getYamlKeyValue(yamlKeyValue, keyName);
        if (childrenKeyValue == null) {
            return Collections.emptyList();
        }

        // class: [test]
        // class:
        //  - "test"
        YAMLValue value = childrenKeyValue.getValue();
        if(value instanceof YAMLSequence) {
          return getYamlArrayValuesAsString((YAMLSequence) value);
        }

        if (isYamlNullScalar(value)) {
            return Collections.emptyList();
        }

        // class: "test"
        String valueText = getYamlKeyValueAsString(yamlKeyValue, keyName);
        if (StringUtils.isBlank(valueText)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(valueText);
    }

    private static boolean isYamlNullScalar(@Nullable YAMLValue value) {
        if (!(value instanceof YAMLScalar scalar) || value.getNode().getElementType() != YAMLElementTypes.SCALAR_PLAIN_VALUE) {
            return false;
        }

        return "~".equals(scalar.getTextValue()) || "null".equalsIgnoreCase(scalar.getTextValue());
    }

    public static @Nullable String getYamlKeyValueAsString(YAMLKeyValue yamlKeyValue, String keyName, boolean ignoreCase) {

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return null;
        }

        return getYamlKeyValueAsString((YAMLCompoundValue) yamlCompoundValue, keyName, ignoreCase);
    }

    public static @Nullable String getYamlKeyValueAsString(@Nullable YAMLCompoundValue yamlCompoundValue, String keyName, boolean ignoreCase) {
        YAMLKeyValue yamlKeyValue1 = getYamlKeyValue(yamlCompoundValue, keyName, ignoreCase);

        if(yamlKeyValue1 == null) {
            return null;
        }

        String valueText = yamlKeyValue1.getValueText();
        if (StringUtils.isBlank(valueText)) {
            return null;
        }

        return valueText;
    }

    public static @Nullable YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, String keyName, boolean ignoreCase) {
        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return null;
        }

        return getYamlKeyValue(yamlCompoundValue, keyName, ignoreCase);
    }


    /**
     * foo:
     *   bar:
     *     |
     *
     *  Will return [foo, bar]
     *
     * todo: YAMLUtil.getFullKey is useless because its not possible to prefix self item value and needs array value
     * @param psiElement any PsiElement inside a key value
     */
    public static List<String> getParentArrayKeys(PsiElement psiElement) {
        List<String> keys = new ArrayList<>();

        YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(yamlKeyValue != null) {
            getParentArrayKeys(yamlKeyValue, keys);
        }

        return keys;
    }


    /**
     * Attach all parent array keys to list (foo:\n bar:): [foo, bar]
     *
     * @param yamlKeyValue current key value context
     * @param key the key list
     */
    public static void getParentArrayKeys(YAMLKeyValue yamlKeyValue, List<String> key) {
        key.add(yamlKeyValue.getKeyText());

        PsiElement yamlCompount = yamlKeyValue.getParent();
        if(yamlCompount instanceof YAMLCompoundValue) {
            PsiElement yamlKeyValueParent = yamlCompount.getParent();
            if(yamlKeyValueParent instanceof YAMLKeyValue) {
                getParentArrayKeys((YAMLKeyValue) yamlKeyValueParent, key);
            }
        }

    }

    public static boolean isRoutingFile(PsiFile psiFile) {
        return psiFile.getName().contains("routing") || psiFile.getVirtualFile().getPath().contains("/routing");
    }

    public static boolean isConfigFile(PsiFile psiFile) {
        return psiFile.getName().contains("config") || psiFile.getVirtualFile().getPath().contains("/config");
    }

    public static boolean isServicesFile(PsiFile psiFile) {
        return psiFile.getName().contains("services") || psiFile.getVirtualFile().getPath().contains("/services");
    }

    public static boolean isInsideServiceDefinition(PsiElement psiElement) {
        return YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement);
    }

    public static boolean isInsideServiceArgumentDefinition(PsiElement psiElement) {
        return isInsideServiceDefinition(psiElement)
            && YamlElementPatternHelper.getInsideKeyValue("arguments", "properties", "calls").accepts(psiElement);
    }

    /**
     * foo.service.method:
     *   class: "ClassName\Foo"
     *   arguments:
     *     - "@twig"
     *     - '@twig'
     *   tags:
     *     -  { name: routing.loader, method: "crossHint<caret>" }
     *
     * ClassName\Foo:
     *   tags:
     *     -  { method: "crossHint<caret>" }
     */
    public static @Nullable String getServiceDefinitionClassFromTagMethod(PsiElement psiElement) {
        PsiElement yamlScalar = psiElement.getParent();
        if(yamlScalar instanceof YAMLScalar) {
            PsiElement yamlKeyValue = yamlScalar.getParent();
            if(yamlKeyValue instanceof YAMLKeyValue) {
                // "{ method: '' }"
                PsiElement yamlMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                if(yamlMapping != null) {
                    PsiElement yamlSequenceItem = yamlMapping.getParent();
                    if(yamlSequenceItem instanceof YAMLSequenceItem) {
                        PsiElement yamlSequence = yamlSequenceItem.getParent();
                        if(yamlSequence instanceof YAMLSequence) {
                            PsiElement yamlKeyValueTags = yamlSequence.getParent();
                            if(yamlKeyValueTags instanceof YAMLKeyValue) {
                                return getClassFromServiceDefinition((YAMLKeyValue) yamlKeyValueTags);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     *  Simplify getting of array psi elements in array or sequence context
     *
     * arguments: [@foo]
     * arguments:
     *   - @foo
     *
     * TODO: can be handled nice know because on new yaml plugin
     */
    public static @Nullable List<PsiElement> getYamlArrayOnSequenceOrArrayElements(YAMLCompoundValue yamlCompoundValue) {
        if (yamlCompoundValue instanceof YAMLSequence) {
            return new ArrayList<>(((YAMLSequence) yamlCompoundValue).getItems());
        }

        if (yamlCompoundValue instanceof YAMLMapping) {
            return new ArrayList<>(((YAMLMapping) yamlCompoundValue).getKeyValues());
        }

        return null;
    }

    /**
     * Finds top most service of any given PsiElement context
     *
     * @param psiElement any PsiElement that is inside an service definition
     */
    public static @Nullable YAMLKeyValue findServiceInContext(PsiElement psiElement) {

        YAMLKeyValue serviceSubKey = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(serviceSubKey == null) {
            return null;
        }

        PsiElement serviceSubKeyCompound = serviceSubKey.getParent();

        // we are inside a YAMLHash element, find most parent array key
        // { name: foo }
        if(serviceSubKeyCompound instanceof YAMLMapping && serviceSubKeyCompound.getNode().getElementType() == YAMLElementTypes.HASH) {
            YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(serviceSubKeyCompound, YAMLKeyValue.class);
            if(yamlKeyValue == null) {
                return null;
            }

            serviceSubKeyCompound = yamlKeyValue.getParent();
        }

        // find array key inside service and check if we are inside "services"
        if(serviceSubKeyCompound instanceof YAMLCompoundValue) {
            PsiElement serviceKey = serviceSubKeyCompound.getParent();
            if(serviceKey instanceof YAMLKeyValue) {
                PsiElement servicesKeyCompound = serviceKey.getParent();
                if(servicesKeyCompound instanceof YAMLCompoundValue) {
                    PsiElement servicesKey = servicesKeyCompound.getParent();
                    if(servicesKey instanceof YAMLKeyValue) {
                        if("services".equals(((YAMLKeyValue) servicesKey).getKeyText())) {
                            return (YAMLKeyValue) serviceKey;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Collect defined service tags on a sequence list
     * - { name: assetic.factory_worker }
     * - [ assetic.factory_worker ]
     *
     * @param yamlKeyValue the service key value to find the "tags" key on
     * @return tag names
     */
    public static Set<String> collectServiceTags(YAMLKeyValue yamlKeyValue) {
        YAMLKeyValue tagsKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, "tags");
        if(tagsKeyValue == null) {
            return Collections.emptySet();
        }

        PsiElement tagsCompound = tagsKeyValue.getValue();
        if(!(tagsCompound instanceof YAMLSequence)) {
            return Collections.emptySet();
        }

        Set<String> tags = new HashSet<>();

        for (YAMLSequenceItem yamlSequenceItem : ((YAMLSequence) tagsCompound).getItems()) {
            YAMLValue value = yamlSequenceItem.getValue();
            if(value instanceof YAMLMapping) {
                // tags:
                //  - {name: foobar}

                String name = YamlHelper.getYamlKeyValueAsString(((YAMLMapping) value), "name");
                if(name != null) {
                    tags.add(name);
                }
            } else if(value instanceof YAMLScalar) {
                // tags: [foobar]

                String textValue = ((YAMLScalar) value).getTextValue();
                if(StringUtils.isNotBlank(textValue)) {
                    tags.add(textValue);
                }
            }
        }

        return tags;
    }

    /**
     * acme_demo.form.type.gender:
     *  class: espend\Form\TypeBundle\Form\FooType
     *  tags:
     *   - { name: foo  }
     */
    public static Collection<YAMLKeyValue> getTaggedServices(YAMLFile yamlFile, String tag) {
        Collection<YAMLKeyValue> yamlKeyValues = new HashSet<>();

        for (YAMLKeyValue yamlServiceKeyValue : YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            Set<String> serviceTagMap = YamlHelper.collectServiceTags(yamlServiceKeyValue);
            if (serviceTagMap.contains(tag)) {
                yamlKeyValues.add(yamlServiceKeyValue);
            }
        }

        return yamlKeyValues;
    }

    /**
     * TODO: use visitor pattern for all tags, we are using them to often
     */
    public static void visitTagsOnServiceDefinition(YAMLKeyValue yamlServiceKeyValue, YamlTagVisitor visitor) {

        YAMLKeyValue tagTag = YamlHelper.getYamlKeyValue(yamlServiceKeyValue, "tags");
        if(tagTag == null) {
            return;
        }

        final YAMLValue tagsValue = tagTag.getValue();
        if(!(tagsValue instanceof YAMLSequence)) {
            return;
        }

        String serviceId = yamlServiceKeyValue.getKeyText();

        for(YAMLSequenceItem yamlSequenceItem: ((YAMLSequence) tagsValue).getItems()) {
            final YAMLValue itemValue = yamlSequenceItem.getValue();

            if(itemValue instanceof final YAMLMapping yamlHash) {
                // tags:
                //  - {name: foobar}

                String tagName = YamlHelper.getYamlKeyValueAsString(yamlHash, "name");
                if(tagName != null) {
                    visitor.visit(new YamlServiceTag(serviceId, tagName, yamlHash));
                }
            } else if(itemValue instanceof YAMLScalar) {
                // tags: [foobar]

                String textValue = ((YAMLScalar) itemValue).getTextValue();
                if(StringUtils.isNotBlank(textValue)) {
                    visitor.visit(new YamlServiceTag(serviceId, textValue, new StaticAttributeResolver("name", textValue)));
                }
            }
        }
    }

    /**
     * Get all children key values of a parent key value
     */
    private static Collection<YAMLKeyValue> getNextKeyValues(YAMLKeyValue yamlKeyValue) {

        final Collection<YAMLKeyValue> yamlKeyValues = new ArrayList<>();
        visitNextKeyValues(yamlKeyValue, yamlKeyValues::add);

        return yamlKeyValues;
    }

    /**
     * Visit all children key values of a parent key value
     */
    private static void visitNextKeyValues(YAMLKeyValue yamlKeyValue, Consumer<YAMLKeyValue> consumer) {
        List<YAMLPsiElement> yamlElements = yamlKeyValue.getYAMLElements();

        // @TODO: multiple?
        if(yamlElements.size() != 1) {
            return;
        }

        YAMLPsiElement next = yamlElements.iterator().next();
        if(!(next instanceof YAMLMapping)) {
            return;
        }

        for (YAMLKeyValue keyValue : ((YAMLMapping) next).getKeyValues()) {
            consumer.accept(keyValue);
        }
    }


    /**
     * Get all key values in first level key visit
     *
     * parameters:
     *  foo: "foo"
     */
    public static Collection<YAMLKeyValue> getQualifiedKeyValuesInFile(YAMLFile yamlFile, String firstLevelKeyToVisit) {
        YAMLKeyValue parameters = YAMLUtil.getQualifiedKeyInFile(yamlFile, firstLevelKeyToVisit);
        if(parameters == null) {
            return Collections.emptyList();
        }

        return getNextKeyValues(parameters);
    }

    /**
     * Visit all key values in first level key
     *
     * parameters:
     *  foo: "foo"
     */
    public static void visitQualifiedKeyValuesInFile(YAMLFile yamlFile, String firstLevelKeyToVisit, Consumer<YAMLKeyValue> consumer) {
        YAMLKeyValue parameters = YAMLUtil.getQualifiedKeyInFile(yamlFile, firstLevelKeyToVisit);
        if(parameters == null) {
            return;
        }

        visitNextKeyValues(parameters, consumer);
    }

    public static @Nullable String getStringValueOfKeyInProbablyMapping(@Nullable YAMLValue node, String keyText) {
        YAMLKeyValue mapping = YAMLUtil.findKeyInProbablyMapping(node, keyText);
        if(mapping == null) {
            return null;
        }

        YAMLValue value = mapping.getValue();
        if(value == null) {
            return null;
        }

        return value.getText();
    }

    public static Collection<YAMLKeyValue> getTopLevelKeyValues(YAMLFile yamlFile) {
        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(yamlFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return Collections.emptyList();
        }

        YAMLValue topLevelValue = yamlDocument.getTopLevelValue();
        if(!(topLevelValue instanceof YAMLMapping)) {
            return Collections.emptyList();
        }

        return ((YAMLMapping) topLevelValue).getKeyValues();
    }

    /**
     * Returns "@foo" value of ["@foo", "fo<caret>o"]
     */
    public static @Nullable String getPreviousSequenceItemAsText(PsiElement psiElement) {
        PsiElement yamlScalar = psiElement.getParent();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return null;
        }

        PsiElement yamlSequence = yamlScalar.getParent();
        if(!(yamlSequence instanceof YAMLSequenceItem)) {
            return null;
        }

        // @TODO: catch new lexer error on empty item [<caret>,@foo] "PsiErrorElement:Sequence item expected"
        YAMLSequenceItem prevSequenceItem = PsiTreeUtil.getPrevSiblingOfType(yamlSequence, YAMLSequenceItem.class);
        if(prevSequenceItem == null) {
            return null;
        }

        YAMLValue value = prevSequenceItem.getValue();
        if(!(value instanceof YAMLScalar)) {
            return null;
        }

        return ((YAMLScalar) value).getTextValue();
    }

    private interface KeyInsertValueFormatter {
        @Nullable String format(YAMLMapping yamlMapping, String chainedKey);
    }

    /**
     * Adds a yaml key on path. This implemention merge values and support nested key values
     * foo:\n  bar: car -> foo.car.foo.bar
     *
     * @param formatter any string think of provide qoute
     */
    public static @Nullable PsiElement insertKeyIntoFile(final YAMLFile yamlFile, KeyInsertValueFormatter formatter, String... keys) {
        final Pair<YAMLKeyValue, String[]> lastKeyStorage = findLastKnownKeyInFile(yamlFile, keys);

        if(lastKeyStorage.getSecond().length == 0) {
            return null;
        }

        YAMLMapping childOfType = null;

        // root condition
        if(lastKeyStorage.getFirst() == null && lastKeyStorage.getSecond().length == keys.length) {
            YAMLValue topLevelValue = yamlFile.getDocuments().get(0).getTopLevelValue();
            if(topLevelValue instanceof YAMLMapping) {
                childOfType = (YAMLMapping) topLevelValue;
            }
        } else if(lastKeyStorage.getFirst() != null) {
            // found a key value in key path append it there
            childOfType = PsiTreeUtil.getChildOfType(lastKeyStorage.getFirst(), YAMLMapping.class);
        }

        if(childOfType == null) {
            return null;
        }

        // pre-generate an empty key value
        String chainedKey = YAMLElementGenerator.createChainedKey(Arrays.asList(lastKeyStorage.getSecond()), YAMLUtil.getIndentInThisLine(childOfType));

        // append value: should be string with right indent for key value
        String value = formatter.format(childOfType, chainedKey);
        if(value != null) {
            chainedKey += value;
        }

        YAMLFile dummyFile = YAMLElementGenerator.getInstance(yamlFile.getProject()).createDummyYamlWithText(chainedKey);

        final YAMLKeyValue next = PsiTreeUtil.collectElementsOfType(dummyFile, YAMLKeyValue.class).iterator().next();
        if(next == null) {
            return null;
        }

        // finally write changes
        final YAMLMapping finalChildOfType = childOfType;
        WriteCommandAction.writeCommandAction(yamlFile.getProject())
            .withName("Key insertion")
            .run(() -> finalChildOfType.putKeyValue(next));

        return childOfType;
    }

    public static @Nullable PsiElement insertKeyIntoFile(final YAMLFile yamlFile, final YAMLKeyValue yamlKeyValue, String... keys) {
        String keyText = yamlKeyValue.getKeyText();

        return insertKeyIntoFile(yamlFile, (yamlMapping, chainedKey) -> {
            String text = yamlKeyValue.getText();

            final String previousIndent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentInThisLine(yamlMapping));

            // split content of array value object;
            // drop first item as getValueText() removes our key indent
            String[] remove = ArrayUtils.remove(text.split("\\r?\\n"), 0);

            List<String> map = ContainerUtil.map(remove, s -> previousIndent + s);

            return "\n" + StringUtils.strip(StringUtils.join(map, "\n"), "\n");
        }, ArrayUtils.add(keys, keyText));
    }

    public static PsiElement insertKeyIntoFile(final YAMLFile yamlFile, final @Nullable String value, String... keys) {
        return insertKeyIntoFile(yamlFile, (yamlMapping, chainedKey) -> " " + value, keys);
    }

    /**
     * Find last known KeyValue of key path, so that we can merge new incoming keys
     */
    private static Pair<YAMLKeyValue, String[]> findLastKnownKeyInFile(YAMLFile file, String... keys) {

        YAMLKeyValue last = null;
        YAMLMapping mapping = ObjectUtil.tryCast(file.getDocuments().get(0).getTopLevelValue(), YAMLMapping.class);

        for (int i = 0; i < keys.length; i++) {
            String s = keys[i];
            if (mapping == null) {
                return Pair.create(last, Arrays.copyOfRange(keys, i, keys.length));
            }

            YAMLKeyValue keyValue = mapping.getKeyValueByKey(s);
            if (keyValue == null) {
                return Pair.create(last, Arrays.copyOfRange(keys, i, keys.length));
            }

            last = keyValue;

            mapping = ObjectUtil.tryCast(keyValue.getValue(), YAMLMapping.class);
        }

        return Pair.create(last, new String[]{});
    }

    /**
     * Bridge to allow YAMLKeyValue adding child key-values elements.
     * Yaml plugin provides key adding only on YAMLMapping
     *
     * ser<caret>vice:
     *   foo: "aaa"
     *
     */
    public static @Nullable YAMLKeyValue putKeyValue(YAMLKeyValue yamlKeyValue, String keyName, String valueText) {

        // create "foo: foo"
        YAMLKeyValue newYamlKeyValue = YAMLElementGenerator.getInstance(yamlKeyValue.getProject())
            .createYamlKeyValue(keyName, valueText);

        YAMLMapping childOfAnyType = PsiTreeUtil.findChildOfAnyType(yamlKeyValue, YAMLMapping.class);
        if(childOfAnyType == null) {
            return null;
        }

        childOfAnyType.putKeyValue(newYamlKeyValue);

        return newYamlKeyValue;
    }

    /**
     * Services id in Symfony 3.3 are allowed to be class names
     * defensive extract by naming strategy
     */
    public static boolean isClassServiceId(String serviceId) {
        // foo.bar
        if(serviceId.contains(".")) {
            return false;
        }

        // A valid namespace can still end in a reserved class name, for example "My\\Sweet\\Class".
        return PhpNameUtil.isValidNamespaceFullName(serviceId, true, PhpLanguageLevel.DEFAULT)
            && PhpNameUtil.isValidClassName(StringUtil.getShortName(serviceId, '\\'));
    }

    /**
     * Returns true if given parameter name is default value for ENV variable.
     */
    public static boolean isDefaultEnvValue(String parameterName) {
        return parameterName.length() > 5 && parameterName.startsWith("env(") && parameterName.endsWith(")");
    }

    /**
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onF<caret>oobar, []]
     *
     * FOOBAR:
     *   calls:
     *      - [onF<caret>oobar, []]
     */
    public static void visitServiceCall(YAMLScalar yamlScalar, Consumer<String> consumer) {
        PsiElement yamlSeq = yamlScalar.getContext();
        if(yamlSeq instanceof YAMLSequenceItem) {
            PsiElement context = yamlSeq.getContext();
            if(context instanceof YAMLSequence) {
                PsiElement yamlSequenceItem = context.getParent();
                if(yamlSequenceItem instanceof YAMLSequenceItem) {
                    PsiElement yamlSeq1 = yamlSequenceItem.getParent();
                    if(yamlSeq1 instanceof YAMLSequence) {
                        PsiElement callYamlKeyValue = yamlSeq1.getParent();
                        if(callYamlKeyValue instanceof YAMLKeyValue) {
                            YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(callYamlKeyValue.getContext(), "class");
                            if(classKeyValue != null) {
                                // "class" key found use this as valid class name
                                String valueText = classKeyValue.getValueText();
                                if(StringUtils.isNotBlank(valueText)) {
                                    consumer.accept(valueText);
                                }
                            } else {
                                // named services; key is our class name
                                PsiElement yamlMapping = callYamlKeyValue.getParent();
                                if(yamlMapping instanceof YAMLMapping) {
                                    PsiElement parent = yamlMapping.getParent();
                                    if(parent instanceof YAMLKeyValue) {
                                        String keyText = ((YAMLKeyValue) parent).getKeyText();
                                        if(!keyText.contains(".") && PhpNameUtil.isValidNamespaceFullName(keyText, PhpLanguageLevel.DEFAULT)) {
                                            consumer.accept(keyText);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get class from server definition; supports shortcut
     *
     * service.id:
     *  class: MyClass
     *
     * MyClass: ~
     */
    private static String getClassFromServiceDefinition(YAMLKeyValue yamlKeyValue) {
        YAMLMapping parentMapping = yamlKeyValue.getParentMapping();

        if(parentMapping != null) {
            YAMLKeyValue classKeyValue = parentMapping.getKeyValueByKey("class");
            if (classKeyValue != null) {
                String valueText = classKeyValue.getValueText();
                if (StringUtils.isNotBlank(valueText)) {
                    return valueText;
                }
            } else {
                // named services; key is our class name
                PsiElement yamlMapping = yamlKeyValue.getParent();
                if(yamlMapping instanceof YAMLMapping) {
                    PsiElement parent = yamlMapping.getParent();
                    if(parent instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) parent).getKeyText();
                        if(StringUtils.isNotBlank(keyText) && !keyText.contains(".") && PhpNameUtil.isValidNamespaceFullName(keyText, PhpLanguageLevel.DEFAULT)) {
                            return keyText;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     *
     * FOOBAR:
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     */
    public static void visitServiceCallArgument(YAMLScalar yamlScalar, Consumer<ParameterVisitor> consumer) {
        PsiElement context = yamlScalar.getContext();
        if(context instanceof YAMLSequenceItem argumentSequenceItem) {
            // [@foobar, @fo<caret>obar]
            if (argumentSequenceItem.getContext() instanceof YAMLSequence yamlCallParameterArray) {
                PsiElement callSequenceItem = yamlCallParameterArray.getContext();
                if(callSequenceItem instanceof YAMLSequenceItem enclosingItem) {
                    if (enclosingItem.getContext() instanceof YAMLSequence yamlCallArray) {
                        PsiElement seqItem = yamlCallArray.getContext();
                        if(seqItem instanceof YAMLSequenceItem) {
                            // - [ setFoo, [@args_bar] ]
                            PsiElement callYamlSeq = seqItem.getContext();
                            if(callYamlSeq instanceof YAMLSequence) {
                                // only given method and args are valid "setFoo, [@args_bar]"
                                List<YAMLSequenceItem> methodParameter = yamlCallArray.getItems();
                                if(methodParameter.size() > 1) {
                                    YAMLValue methodNameElement = methodParameter.get(0).getValue();
                                    if(methodNameElement instanceof YAMLScalar) {
                                        String methodName = ((YAMLScalar) methodNameElement).getTextValue();
                                        if(StringUtils.isNotBlank(methodName)) {
                                            PsiElement callYamlKeyValue = callYamlSeq.getContext();
                                            if(callYamlKeyValue instanceof YAMLKeyValue) {
                                                String classFromServiceDefinition = getClassFromServiceDefinition((YAMLKeyValue) callYamlKeyValue);
                                                if(classFromServiceDefinition != null) {
                                                    consumer.accept(new ParameterVisitor(
                                                        classFromServiceDefinition,
                                                        methodName,
                                                        PsiElementUtils.getPrevSiblingsOfType(argumentSequenceItem, PlatformPatterns.psiElement(YAMLSequenceItem.class)).size())
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Consumer for method parameter match
     *
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     */
    public static void visitServiceCallArgumentMethodIndex(YAMLScalar yamlScalar, Consumer<Parameter> consumer) {
        YamlHelper.visitServiceCallArgument(yamlScalar, new ParameterResolverConsumer(yamlScalar.getProject(), consumer));
    }

    /**
     * Get false or true value for given key or null if not found or invalid value was found
     */
    public static @Nullable Boolean getYamlKeyValueAsBoolean(YAMLKeyValue yamlKeyValue, String key) {
        YAMLKeyValue autowire = YamlHelper.getYamlKeyValue(yamlKeyValue, key);
        if(autowire == null) {
            return null;
        }

        YAMLValue value = autowire.getValue();
        if(!(value instanceof YAMLScalar)) {
            return null;
        }

        String textValue = ((YAMLScalar) value).getTextValue().toLowerCase();
        return switch (textValue) {
            case "false" -> false;
            case "true" -> true;
            default -> null;
        };
    }

    /**
     * Try to find a valid indent value, which are spaces which we need to fill
     */
    public static int getIndentSpaceForFile(YAMLFile yamlFile) {
        List<YAMLDocument> documents = yamlFile.getDocuments();

        YAMLMapping mapping = ObjectUtil.tryCast(documents.get(0).getTopLevelValue(), YAMLMapping.class);
        if(mapping != null) {
            // first first INDENT element in mapping
            PsiElementPattern.Capture<PsiElement> pattern = PlatformPatterns
                .psiElement(YAMLTokenTypes.INDENT)
                .with(new PsiElementPatternCondition());

            for (YAMLPsiElement yamlPsiElement : mapping.getKeyValues()) {
                // get first value
                PsiElement firstChild = yamlPsiElement.getFirstChild();
                if(firstChild == null) {
                    continue;
                }

                // first valid INDENT
                PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(firstChild, pattern);
                if(nextSiblingOfType != null && nextSiblingOfType.getTextLength() > 0) {
                    return nextSiblingOfType.getTextLength();
                }
            }
        }

        // default value
        return 4;
    }

    public static boolean isStringValue(PsiElement psiElement) {
        // @TODO use new YAMLScalar element
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).accepts(psiElement)
            ;
    }

    private static class PsiElementPatternCondition extends PatternCondition<PsiElement> {
        PsiElementPatternCondition() {
            super("Indent Check");
        }

        @Override
        public boolean accepts(PsiElement psiElement, ProcessingContext processingContext) {
            return psiElement.getNextSibling() instanceof YAMLMapping;
        }
    }

    public static Collection<PhpClass> getPhpClassesInYamlFile(YAMLFile yamlFile, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        Collection<PhpClass> phpClasses = new HashSet<>(); //TODO

        for (YAMLKeyValue keyValue : YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            YAMLValue value = keyValue.getValue();
            if (value instanceof YAMLMapping) {
                // foo.bar:
                //    classes: ...
                String serviceId = ServiceContainerUtil.getServiceClassFromServiceMapping((YAMLMapping) value);
                if (StringUtils.isNotBlank(serviceId)) {
                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(yamlFile.getProject(), serviceId, lazyServiceCollector);
                    if (serviceClass != null) {
                        phpClasses.add(serviceClass);
                    }
                }

                // My<caret>Class\:
                //    resource: ...
                if (keyValue.getKeyText().endsWith("\\")) {
                    phpClasses.addAll(YamlHelper.getNamespaceResourcesClasses(keyValue));
                }
            } else if(isPlainText(value)) {
                // Foo\Bar: ~
                String text = keyValue.getKeyText();

                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text)) {
                    phpClasses.addAll(PhpElementsUtil.getClassesInterface(yamlFile.getProject(), text));
                }
            }
        }

        return phpClasses;
    }

    /**
     * My<caret>Class\:
     *  resource: '....'
     *  exclude: '....'
     */
    public static Collection<PhpClass> getNamespaceResourcesClasses(YAMLKeyValue yamlKeyValue) {
        PsiFile containingFile = yamlKeyValue.getContainingFile();
        if (containingFile == null) {
            return Collections.emptyList();
        }

        VirtualFile containerFile = containingFile.getVirtualFile();
        if (containerFile == null) {
            return Collections.emptyList();
        }

        var service = ServiceIndexUtil.findServiceDefinition(yamlKeyValue.getProject(), containerFile, yamlKeyValue.getKeyText());
        if (service == null || service.getResource().isEmpty()) {
            return Collections.emptyList();
        }

        return ServiceIndexUtil.getClassesForServiceDefinition(yamlKeyValue.getProject(), containerFile, service).stream()
            .filter(PhpClass.class::isInstance)
            .map(PhpClass.class::cast)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * key: !my_tag <caret>
     */
    public static boolean isElementAfterYamlTag(PsiElement psiElement) {
        if (!(psiElement instanceof LeafPsiElement)) {
            return false;
        }

        // key: !my_tag <caret>\n
        if (((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.EOL) {
            // consulo-port: PsiTreeUtil.getDeepestVisibleLast is missing in Consulo, inlined platform implementation
            PsiElement prevElement = PsiTreeUtil.getDeepestLast(psiElement);
            if (StringUtil.isEmptyOrSpaces(prevElement.getText())) {
                prevElement = PsiTreeUtil.prevVisibleLeaf(prevElement);
            }
            if (prevElement instanceof LeafPsiElement) {
                if (((LeafPsiElement) prevElement).getElementType() == YAMLTokenTypes.TAG) {
                    return ((LeafPsiElement) prevElement).getText().startsWith("!");
                }
            }
        }

        return PsiTreeUtil.findSiblingBackward(psiElement, YAMLTokenTypes.TAG, null) != null;
    }

    /**
     * key: foo\n
     * <caret>
     */
    public static boolean isElementAfterEol(PsiElement psiElement) {
        if (isPlainText(psiElement.getParent())) {
            psiElement = psiElement.getParent();
        }
        return PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(YAMLTokenTypes.EOL)) != null;
    }


    /**
     * A plain (unquoted, not block) scalar, the {@code YAMLPlainTextImpl} of the not exported YAML implementation
     */
    public static boolean isPlainText(@Nullable PsiElement element) {
        return element instanceof YAMLScalar && element.getNode().getElementType() == YAMLElementTypes.SCALAR_PLAIN_VALUE;
    }
}
