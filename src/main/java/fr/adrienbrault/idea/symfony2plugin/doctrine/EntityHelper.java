package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityHelper {

    public static final ExtensionPointName<DoctrineModelProvider> MODEL_POINT_NAME = ExtensionPointName.create(DoctrineModelProvider.class);

    private static final Key<CachedValue<Collection<DoctrineModelCached>>> SYMFONY_DOCTRINE_MODEL_CACHE = new Key<>("SYMFONY_DOCTRINE_MODEL_CACHE");

    final public static String[] ANNOTATION_FIELDS = new String[] {
        "\\Doctrine\\ORM\\Mapping\\Column",
        "\\Doctrine\\ORM\\Mapping\\OneToOne",
        "\\Doctrine\\ORM\\Mapping\\ManyToOne",
        "\\Doctrine\\ORM\\Mapping\\OneToMany",
        "\\Doctrine\\ORM\\Mapping\\ManyToMany",
    };

    final public static Set<String> RELATIONS = new HashSet<>(Arrays.asList("manytoone", "manytomany", "onetoone", "onetomany"));

    /**
     * Resolve shortcut and namespaces classes for current phpclass and attached modelname
     */
    @Nullable
    public static String getAnnotationRepositoryClass(PhpClass phpClass, String modelName) {
        // \ns\Class fine we dont need to resolve classname we are in global context
        if(modelName.startsWith("\\")) {
            return modelName;
        }

        return AnnotationBackportUtil.getFqnClassNameFromScope(phpClass, modelName);
    }

    /**
     * Search for a repository class of a model
     *
     * @param project Current project
     * @param shortcutName "\Class\Name" or "FooBundle:Name"
     */
    @Nullable
    public static PhpClass getEntityRepositoryClass(Project project, String shortcutName) {
        PhpClass phpClass = resolveShortcutName(project, shortcutName);
        if(phpClass == null) {
            return null;
        }

        String presentableFQN = phpClass.getPresentableFQN();
        PhpClass classRepository = DoctrineMetadataUtil.getClassRepository(project, presentableFQN);
        if(classRepository != null) {
            return classRepository;
        }

        // search on annotations
        PhpDocComment docAnnotation = phpClass.getDocComment();
        if(docAnnotation != null) {
            // search for repositoryClass="Foo\Bar\RegisterRepository"; repositoryClass=Foo\Bar\RegisterRepository::class
            // @MongoDB\Document; @ORM\Entity
            Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.extractAnnotations(phpClass, docAnnotation);
            if (!classRepositoryPair.isEmpty()) {
                DoctrineClassMetadata next = classRepositoryPair.iterator().next();
                if (next.repositoryClass() != null) {
                    return PhpElementsUtil.getClassInterface(project, next.repositoryClass());
                }
            }
        }

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(project).getContainingBundle(phpClass);
        if(symfonyBundle != null) {
            PhpClass repositoryClass = getEntityRepositoryClass(project, symfonyBundle, presentableFQN);
            if(repositoryClass != null) {
                return repositoryClass;
            }
        }

        // old __CLASS__ Repository type
        // @TODO remove this fallback when we implemented all cases
        return resolveShortcutName(project, shortcutName + "Repository");
    }

    public static List<DoctrineModelField> getModelFieldsSet(YAMLKeyValue yamlKeyValue) {

        List<DoctrineModelField> fields = new ArrayList<>();

        for(Map.Entry<String, YAMLKeyValue> entry: getYamlModelFieldKeyValues(yamlKeyValue).entrySet()) {
            List<DoctrineModelField> fieldSet = getYamlDoctrineFields(entry.getKey(), entry.getValue());
            if(fieldSet != null) {
                fields.addAll(fieldSet);
            }
        }

        return fields;
    }


    @Nullable
    public static List<DoctrineModelField> getYamlDoctrineFields(String keyName, @Nullable YAMLKeyValue yamlKeyValue) {

        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(yamlCompoundValue == null) {
            return null;
        }

        List<DoctrineModelField> modelFields = new ArrayList<>();
        for(YAMLKeyValue yamlKey: PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class)) {
            String fieldName = YamlHelper.getYamlKeyName(yamlKey);
            if(fieldName != null) {
                DoctrineModelField modelField = new DoctrineModelField(fieldName);
                modelField.addTarget(yamlKey);
                attachYamlFieldTypeName(keyName, modelField, yamlKey);
                modelFields.add(modelField);
            }
        }

        return modelFields;
    }

    public static void attachYamlFieldTypeName(String keyName, DoctrineModelField doctrineModelField, YAMLKeyValue yamlKeyValue) {

        if("fields".equals(keyName) || "id".equals(keyName)) {

            YAMLKeyValue yamlType = YamlHelper.getYamlKeyValue(yamlKeyValue, "type");
            if(yamlType != null) {
                doctrineModelField.setTypeName(yamlType.getValueText());
            }

            YAMLKeyValue yamlColumn = YamlHelper.getYamlKeyValue(yamlKeyValue, "column");
            if(yamlColumn != null) {
                doctrineModelField.setColumn(yamlColumn.getValueText());
            }

            // Enum type
            YAMLKeyValue yamlEnumType = YamlHelper.getYamlKeyValue(yamlKeyValue, "enumType");
            if(yamlEnumType != null) {
                String enumType = yamlEnumType.getValueText();
                if(!enumType.isEmpty()) {
                    doctrineModelField.setEnumType("\\" + org.apache.commons.lang3.StringUtils.stripStart(enumType, "\\"));
                }
            }

            return;
        }

        if(RELATIONS.contains(keyName.toLowerCase())) {
            YAMLKeyValue targetEntity = YamlHelper.getYamlKeyValue(yamlKeyValue, "targetEntity");
            if(targetEntity != null) {
                doctrineModelField.setRelationType(keyName);
                doctrineModelField.setRelation(getOrmClass(yamlKeyValue.getContainingFile(), targetEntity.getValueText()));
            }
        }

    }

    public static String getOrmClass(PsiFile psiFile, String className) {

        // force global namespace not need to search for class
        if(className.startsWith("\\")) {
            return className;
        }

        String entityName = null;

        // espend\Doctrine\ModelBundle\Entity\Bike:
        // ...
        // targetEntity: Foo
        if(psiFile instanceof YAMLFile) {
            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument != null) {
                YAMLKeyValue entityKeyValue = PsiTreeUtil.getChildOfType(yamlDocument, YAMLKeyValue.class);
                if(entityKeyValue != null) {
                    entityName = entityKeyValue.getKeyText();
                }
            }
        } else if(psiFile instanceof XmlFile) {

            XmlTag rootTag = ((XmlFile) psiFile).getRootTag();
            if(rootTag != null) {
                XmlTag entity = rootTag.findFirstSubTag("entity");
                if(entity != null) {
                    String name = entity.getAttributeValue("name");
                    if(org.apache.commons.lang3.StringUtils.isBlank(name)) {
                        entityName = name;
                    }
                }
            }
        }

        if(entityName == null) {
            return className;
        }

        // trim class name
        int lastBackSlash = entityName.lastIndexOf("\\");
        if(lastBackSlash > 0) {
            String fqnClass = entityName.substring(0, lastBackSlash + 1) + className;
            if(PhpElementsUtil.getClass(psiFile.getProject(), fqnClass) != null) {
                return fqnClass;
            }
        }

        return className;
    }

    public static Map<String, YAMLKeyValue> getYamlModelFieldKeyValues(YAMLKeyValue yamlKeyValue) {
        Map<String, YAMLKeyValue> keyValueCollection = new HashMap<>();

        for(String fieldMap: new String[] { "id", "fields", "manyToOne", "oneToOne", "manyToMany", "oneToMany"}) {
            YAMLKeyValue targetYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, fieldMap, true);
            if(targetYamlKeyValue != null) {
                keyValueCollection.put(fieldMap, targetYamlKeyValue);
            }
        }

        return keyValueCollection;
    }

    public static PsiElement[] getModelFieldTargets(PhpClass phpClass, String fieldName) {
        DoctrineMetadataModel modelFields = DoctrineMetadataUtil.getModelFields(phpClass.getProject(), phpClass.getPresentableFQN());
        if(modelFields != null) {
            for (DoctrineModelField field : modelFields.getFields()) {
                if(field.getName().equals(fieldName) && !field.getTargets().isEmpty()) {
                    return field.getTargets().toArray(new PsiElement[0]);
                }
            }
        }

        // Fallback: getter method
        String methodName = "get" + StringUtils.camelize(fieldName.toLowerCase(), false);
        Method method = phpClass.findMethodByName(methodName);
        if(method != null) {
            return new PsiElement[]{method};
        }

        return PsiElement.EMPTY_ARRAY;
    }

    public static Collection<DoctrineModelField> getModelFields(PhpClass phpClass) {
        String presentableFQN = phpClass.getPresentableFQN();
        DoctrineMetadataModel fields = DoctrineMetadataUtil.getModelFields(phpClass.getProject(), presentableFQN);
        if(fields != null) {
            return fields.getFields();
        }
        return Collections.emptyList();
    }

    public static List<DoctrineModelField> getXmlModelFields(XmlTag modelTag) {

        List<DoctrineModelField> modelFields = new ArrayList<>();

        for (XmlTag xmlTag : new ArrayList<XmlTag>() {{
            addAll(Arrays.asList(modelTag.findSubTags("field")));
            addAll(Arrays.asList(modelTag.findSubTags("id")));
        }}) {

            String name = xmlTag.getAttributeValue("name");
            if(org.apache.commons.lang3.StringUtils.isBlank(name)) {
                continue;
            }

            DoctrineModelField field = new DoctrineModelField(name);

            field.addTarget(xmlTag);

            String column = xmlTag.getAttributeValue("column");
            if(org.apache.commons.lang3.StringUtils.isNotBlank(column)) {
                field.setColumn(column);
            }

            String type = xmlTag.getAttributeValue("type");
            if(org.apache.commons.lang3.StringUtils.isNotBlank(type)) {
                field.setTypeName(type);
            }

            // Enum type
            String enumType = xmlTag.getAttributeValue("enum-type");
            if(org.apache.commons.lang3.StringUtils.isNotBlank(enumType)) {
                field.setEnumType("\\" + org.apache.commons.lang3.StringUtils.stripStart(enumType, "\\"));
            }

            modelFields.add(field);
        }

        for(String s: new String[] {"one-to-one", "one-to-many", "many-to-many", "many-to-one"}) {
            for (XmlTag xmlTag : modelTag.findSubTags(s)) {

                String targetEntity = xmlTag.getAttributeValue("target-entity");
                if(targetEntity == null) {
                    continue;
                }

                String field = xmlTag.getAttributeValue("field");
                if(field == null) {
                    continue;
                }

                DoctrineModelField entityField = new DoctrineModelField(field);
                entityField.addTarget(xmlTag);

                // find namespace
                entityField.setRelation(getOrmClass(modelTag.getContainingFile(), targetEntity));

                entityField.setRelationType(StringUtils.camelize(s.replace("-", "_")));
                modelFields.add(entityField);
            }
        }

        return modelFields;
    }

    @Nullable
    private static PhpClass getEntityRepositoryClass(Project project, SymfonyBundle symfonyBundle, String classFqnName) {

        // some default bundle search path
        // Bundle/Resources/config/doctrine/Product.orm.yml
        // Bundle/Resources/config/doctrine/Product.mongodb.yml
        List<String[]> managerConfigs = new ArrayList<>();
        managerConfigs.add(new String[] { "Entity", "orm"});
        managerConfigs.add(new String[] { "Document", "mongodb"});

        for(String[] managerConfig: managerConfigs) {
            String entityName = classFqnName.substring(symfonyBundle.getNamespaceName().length() - 1);
            if(entityName.startsWith(managerConfig[0] + "\\")) {
                entityName =  entityName.substring((managerConfig[0] + "\\").length());
            }

            // entities in sub folder: 'Foo\Bar' -> 'Foo.Bar.orm.yml'
            String entityFile = "Resources/config/doctrine/" + entityName.replace("\\", ".") + String.format(".%s.yml", managerConfig[1]);
            VirtualFile virtualFile = symfonyBundle.getRelative(entityFile);
            if(virtualFile != null) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null) {

                    // search for "repositoryClass: Foo\Bar\RegisterRepository" also provide quoted values
                    Matcher matcher = Pattern.compile("[\\s]*repositoryClass:[\\s]*[\"|']*(.*)[\"|']*").matcher(psiFile.getText());
                    if (matcher.find()) {
                        return PhpElementsUtil.getClass(PhpIndex.getInstance(project), matcher.group(1));
                    }

                    // we found entity config so no other check needed
                    return null;
                }

            }
        }

        return null;
    }

    @Nullable
    public static PhpClass resolveShortcutName(Project project, String shortcutName) {
        return resolveShortcutName(project, shortcutName, DoctrineTypes.Manager.ORM, DoctrineTypes.Manager.MONGO_DB, DoctrineTypes.Manager.COUCH_DB);
    }

    /**
     *
     * @param project PHPStorm projects
     * @param shortcutName name as MyBundle\Entity\Model or MyBundle:Model
     * @return null|PhpClass
     */
    @Nullable
    public static PhpClass resolveShortcutName(Project project, @Nullable String shortcutName, DoctrineTypes.Manager... managers) {
        if(shortcutName == null) {
            return null;
        }

        // we dont need to resolve bundle name, use class name
        if (!shortcutName.contains(":")) {
            return PhpElementsUtil.getClassInterface(project, shortcutName);
        }

        // resolve:
        // MyBundle:Model -> MyBundle\Entity\Model
        // MyBundle:Folder\Model -> MyBundle\Entity\Folder\Model

        List<DoctrineTypes.Manager> managerList = Arrays.asList(managers);

        // collect entitymanager namespaces on bundle or container file
        Map<String, String> em = new HashMap<>();
        if(managerList.contains(DoctrineTypes.Manager.ORM)) {
            Map<String, String> entityNameMap = ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap();
            em.putAll(entityNameMap);
            em.putAll(EntityHelper.getWeakBundleNamespaces(project, entityNameMap, "Entity"));
        }

        Map<String, String> odm = new HashMap<>();
        if(managerList.contains(DoctrineTypes.Manager.MONGO_DB) || managerList.contains(DoctrineTypes.Manager.COUCH_DB)) {
            Map<String, String> documentMap = ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap();
            odm.putAll(documentMap);
            odm.putAll(EntityHelper.getWeakBundleNamespaces(project, documentMap, "Document"));
        }

        // split bundle and model name
        int firstDirectorySeparatorIndex = shortcutName.indexOf(":");
        String bundlename = shortcutName.substring(0, firstDirectorySeparatorIndex);
        String entityName = shortcutName.substring(firstDirectorySeparatorIndex + 1);

        // conditional find namespace on manager paths
        for(Map<String, String> map: Arrays.asList(em, odm)) {
            String namespace = map.get(bundlename);
            if(namespace == null) {
                continue;
            }

            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, namespace + "\\" + entityName);
            if(classInterface != null) {
                return classInterface;
            }
        }

        return null;
    }

    public static DoctrineTypes.@Nullable Manager getManager(MethodReference methodReference) {

        PhpPsiElement phpTypedElement = methodReference.getFirstPsiChild();
        if(!(phpTypedElement instanceof PhpTypedElement)) {
            return null;
        }

        for(String typeString: PhpIndex.getInstance(methodReference.getProject()).completeType(methodReference.getProject(), ((PhpTypedElement) phpTypedElement).getType(), new HashSet<>()).getTypes()) {
            for(Map.Entry<DoctrineTypes.Manager, String> entry: DoctrineTypes.getManagerInstanceMap().entrySet()) {
                if(PhpElementsUtil.isInstanceOf(methodReference.getProject(), typeString, entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    /**
     * Resolve navigation targets for a Doctrine entity: its repository class, the entity class itself, and any metadata files.
     *
     * @param entityName fully qualified class name or bundle shortcut (e.g. "App\Entity\Foo" or "AppBundle:Foo")
     */
    public static PsiElement[] getModelPsiTargets(Project project, String entityName) {
        List<PsiElement> results = new ArrayList<>();

        PhpClass phpClass = EntityHelper.getEntityRepositoryClass(project, entityName);
        if(phpClass != null) {
            results.add(phpClass);
        }

        PhpClass entity = EntityHelper.resolveShortcutName(project, entityName);
        if(entity != null) {
            results.add(entity);

            Collection<VirtualFile> metadataFiles = DoctrineMetadataUtil.findMetadataFiles(
                project,
                org.apache.commons.lang3.StringUtils.stripStart(entity.getPresentableFQN(), "\\")
            );
            for (VirtualFile metadataFile : metadataFiles) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(metadataFile);
                if(psiFile != null) {
                    results.add(psiFile);
                }
            }
        }

        return results.toArray(new PsiElement[0]);
    }

    public static void attachAnnotationInformation(PhpClass phpClass, Field field, DoctrineModelField doctrineModelField, Map<String, String> useImportMap) {
        // we already have that without regular expression
        // @TODO: de.espend.idea.php.annotation.util.AnnotationUtil.getPhpDocCommentAnnotationContainer()
        // fully require plugin now?

        // get some more presentable completion information
        // dont resolve docblocks; just extract them from doc comment
        PhpDocComment docBlock = field.getDocComment();

        if(docBlock == null) {
            return;
        }

        String text = docBlock.getText();

        // column type
        Matcher matcher = Pattern.compile("type[\\s]*=[\\s]*[\"|']([\\w_\\\\]+)[\"|']").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setTypeName(matcher.group(1));
        }

        // relation type
        matcher = Pattern.compile("((Many|One)To(Many|One))\\(").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setRelationType(matcher.group(1));
            String clazz = resolveDoctrineLikePropertyClass(phpClass, text, "targetEntity", aVoid -> useImportMap);

            if (clazz != null) {
                doctrineModelField.setRelation(clazz);
            } else {
                // @TODO: external split
                // FLOW shortcut:
                // @var "\DateTime" is targetEntity
                PhpDocParamTag varTag = docBlock.getVarTag();
                if(varTag != null) {
                    String type = varTag.getType().toString();
                    if(org.apache.commons.lang3.StringUtils.isNotBlank(type)) {
                        doctrineModelField.setRelation(type);
                    }
                }
            }
        }

        matcher = Pattern.compile("Column\\(").matcher(text);
        if (matcher.find()) {
            matcher = Pattern.compile("name\\s*=\\s*\"(\\w+)\"").matcher(text);
            if(matcher.find()) {
                doctrineModelField.setColumn(matcher.group(1));
            }

        }

    }

    private record DoctrineModelCached(String phpClass, @Nullable String doctrineShortcut, @Nullable String doctrineNamespace) {
    }

    private static Collection<DoctrineModelCached> getModelClassesInner(final Project project) {
        HashMap<String, String> shortcutNames = new HashMap<>() {{
            putAll(ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap());
            putAll(ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap());
        }};

        for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundles()) {
            for(String s : new String[] {"Entity", "Document", "CouchDocument"}) {
                String namespace = symfonyBundle.getNamespaceName() + s;
                if(symfonyBundle.getRelative(s) != null || !PhpIndex.getInstance(project).getNamespacesByName(namespace).isEmpty()) {
                    shortcutNames.put(symfonyBundle.getName(), namespace);
                }
            }
        }

        // class fqn fallback
        Collection<DoctrineModel> doctrineModels = getModelClasses(project, shortcutNames);
        for (PhpClass phpClass : DoctrineMetadataUtil.getModels(project)) {
            if (containsDoctrineModelClass(doctrineModels, phpClass)) {
                continue;
            }

            doctrineModels.add(new DoctrineModel(phpClass));
        }

        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, new ArrayList<>());
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                doctrineModels.add(new DoctrineModel(doctrineModel.getPhpClass(), doctrineModel.getName()));
            }
        }

        return doctrineModels.stream().map(
            doctrineModel -> new DoctrineModelCached(doctrineModel.getPhpClass().getFQN(), doctrineModel.getDoctrineShortcut(), doctrineModel.getDoctrineNamespace())
        ).toList();
    }

    /**
     * One PhpClass can have multiple targets and names @TODO: refactor
     */
    public static Collection<DoctrineModel> getModelClasses(final Project project) {
        Collection<DoctrineModelCached> modelClasses = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_DOCTRINE_MODEL_CACHE,
            () -> CachedValueProvider.Result.create(getModelClassesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<DoctrineModel> doctrineModels = new ArrayList<>();
        for (DoctrineModelCached doctrineModelCached : modelClasses) {
            Collection<PhpClass> classesByFQN = phpIndex.getClassesByFQN(doctrineModelCached.phpClass);
            if (classesByFQN.isEmpty()) {
                continue;
            }

            doctrineModels.add(new DoctrineModel(classesByFQN.iterator().next(), doctrineModelCached.doctrineShortcut, doctrineModelCached.doctrineNamespace));
        }

        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, new ArrayList<>());
        for (DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for (DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                doctrineModels.add(new DoctrineModel(doctrineModel.getPhpClass(), doctrineModel.getName()));
            }
        }

        return doctrineModels;
    }

    private static boolean containsDoctrineModelClass(Collection<DoctrineModel> models, PhpClass phpClass) {
        for (DoctrineModel doctrineModel : models) {
            if(PhpElementsUtil.isEqualClassName(doctrineModel.getPhpClass(), phpClass)) {
                return true;
            }
        }

        return false;
    }

    public static Collection<DoctrineModel> getModelClasses(Project project, Map<String, String> shortcutNames) {
        PhpClass repositoryInterface = PhpElementsUtil.getInterface(PhpIndex.getInstance(project), DoctrineTypes.REPOSITORY_INTERFACE);

        if (repositoryInterface == null) {
            repositoryInterface = PhpElementsUtil.getInterface(PhpIndex.getInstance(project), "\\Doctrine\\Persistence\\ObjectRepository");
        }

        Collection<DoctrineModel> models = new ArrayList<>();
        for (Map.Entry<String, String> entry : shortcutNames.entrySet()) {
            for (PhpClass phpClass: PhpIndexUtil.getPhpClassInsideNamespace(project, entry.getValue(), true)) {
                if (repositoryInterface != null && !isEntity(phpClass, repositoryInterface)) {
                    continue;
                }

                models.add(new DoctrineModel(phpClass, entry.getKey(), entry.getValue()));
            }
        }

        return models;
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {
        if(entityClass.isAbstract() || entityClass.isInterface() || entityClass.isTrait()) {
            return false;
        }

        return !PhpElementsUtil.isInstanceOf(entityClass, repositoryClass);
    }

    public static Map<String, String> getWeakBundleNamespaces(Project project, Map<String, String> entityNameMap, String subFolder) {

        Map<String, String> missingMap = new HashMap<>();

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(project).getBundles();
        for(SymfonyBundle symfonyBundle: symfonyBundles) {
            if(symfonyBundle.isTestBundle()) {
                continue;
            }

            // namespace already known
            String bundleName = symfonyBundle.getName();
            if(entityNameMap.containsKey(bundleName)) {
               continue;
            }

            // find namepsace on file or class index
            String namespace = symfonyBundle.getNamespaceName() + subFolder;
            if(symfonyBundle.getRelative(subFolder) != null || !PhpIndex.getInstance(project).getNamespacesByName(namespace).isEmpty()) {
                missingMap.put(bundleName, namespace);
            }
        }

        return missingMap;
    }

    /**
     * Resolve class instances from annotation based on the class context.
     *
     * 'repositoryClass="Foo"', 'repostoryClass=Foo::class'
     *
     * - "Foo\Bar" is resolved as a "\Foo\Bar" on Doctrine
     * - "Bar" append to the namespace name
     */
    public static String resolveDoctrineLikePropertyClass(PhpClass phpClass, String text, String propertyName, Function<Void, Map<String, String>> useImportMap) {
        Map<String, Matcher> matches = new HashMap<>() {{
            put("string", Pattern.compile(propertyName + "\\s*=\\s*\"([^\"]*)\"").matcher(text)); // targetEntity="Foobar"
            put("class", Pattern.compile(propertyName + "\\s*=\\s*([^\\s:]*)::class").matcher(text));  // targetEntity=Foobar::class
        }};

        for (Map.Entry<String, Matcher> pair : matches.entrySet()) {
            Matcher targetEntityMatch = pair.getValue();
            if (!targetEntityMatch.find()) {
                continue;
            }

            String targetEntity = targetEntityMatch.group(1);
            if (org.apache.commons.lang3.StringUtils.isBlank(targetEntity)) {
                continue;
            }

            if ("class".equals(pair.getKey())) {
                return AnnotationBackportUtil.getFqnClassNameFromScope(phpClass, targetEntity, useImportMap.apply(null));
            }

            return targetEntity.contains("\\")
                ? targetEntity // "Foo\Bar" is resolved as a "\Foo\Bar" on Doctrine
                : phpClass.getNamespaceName() + targetEntity; // "Bar" append to the namespace name
        }

        return null;
    }
}
