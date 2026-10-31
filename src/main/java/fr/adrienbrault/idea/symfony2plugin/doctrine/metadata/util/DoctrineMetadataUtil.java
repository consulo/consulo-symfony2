package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup.DoctrineRepositoryLookupElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataUtil {

    private static final Key<CachedValue<Set<String>>> CLASS_KEYS = new Key<>("CLASS_KEYS");
    private static final Key<CachedValue<Collection<Pair<String, String>>>> DOCTRINE_TABLES_CACHE = new Key<>("DOCTRINE_TABLES_CACHE");

    private static final DoctrineMappingDriverInterface[] MAPPING_DRIVERS = new DoctrineMappingDriverInterface[] {
        new DoctrinePhpMappingDriver(), // cross-file content, no simple cache
        new DoctrinePhpAttributeMappingDriver(), // cross-file content, no simple cache
        new DoctrineMappingDriverFileCacheDecorator(new DoctrineXmlMappingDriver()),
        new DoctrineMappingDriverFileCacheDecorator(new DoctrineYamlMappingDriver()),
    };

    public static Collection<LookupElement> getObjectRepositoryLookupElements(Project project) {
        Collection<PhpClass> collection = new ArrayList<>();

        collection.addAll(PhpIndexUtil.getAllSubclasses(project, "\\Doctrine\\Common\\Persistence\\ObjectRepository"));
        collection.addAll(PhpIndexUtil.getAllSubclasses(project, "\\Doctrine\\Persistence\\ObjectRepository"));

        return new ArrayList<>(DoctrineRepositoryLookupElement.create(collection));
    }

    /**
     * Try to find repository class on models scope on its metadata definition
     */
    @Nullable
    public static PhpClass getClassRepository(final Project project, final String className) {

        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, className, GlobalSearchScope.allScope(project))) {

            final String[] phpClass = {null};

            FileBasedIndex.getInstance().processValues(DoctrineMetadataFileStubIndex.KEY, className, virtualFile, (virtualFile1, model) -> {
                if (phpClass[0] != null  || model == null || model.getRepositoryClass() == null) {
                    return true;
                }

                // piping value out of this index thread
                phpClass[0] = model.getRepositoryClass();

                return true;
            }, GlobalSearchScope.allScope(project));

            if(phpClass[0] != null) {
                return PhpElementsUtil.getClassInsideNamespaceScope(project, className, phpClass[0]);
            }
        }

        return null;
    }

    /**
     * Find Doctrine metadata files (YAML, XML, PHP) for the given entity class.
     *
     * @param className fully qualified class name, with or without leading backslash
     */
    public static Collection<VirtualFile> findMetadataFiles(Project project, String className) {
        className = StringUtils.stripStart(className, "\\");
        final Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(DoctrineMetadataFileStubIndex.KEY, new HashSet<>(Collections.singletonList(className)), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        return virtualFiles;
    }

    /**
     * Find Doctrine metadata files for the entity that uses the given repository class.
     *
     * @param phpClass the repository PhpClass
     */
    public static Collection<VirtualFile> findMetadataForRepositoryClass(PhpClass phpClass) {
        return findMetadataForRepositoryClass(
            phpClass.getProject(),
            StringUtils.stripStart(phpClass.getPresentableFQN(), "\\")
        );
    }

    private static final Key<CachedValue<Map<String, Collection<String>>>> DOCTRINE_REPOSITORY_CACHE;

    static {
        DOCTRINE_REPOSITORY_CACHE = new Key<>("DOCTRINE_REPOSITORY_CACHE");
    }

    /**
     * Find Doctrine metadata files for the entity that references the given repository class name.
     *
     * @param repositoryClass fully qualified repository class name without leading backslash
     */
    public static Collection<VirtualFile> findMetadataForRepositoryClass(final Project project, String repositoryClass) {
        Map<String, Collection<String>> cache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            DOCTRINE_REPOSITORY_CACHE,
            () -> {
                Map<String, Collection<String>> repositoryMap = new HashMap<>();
                for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
                    for (DoctrineModelInterface repositoryDefinition : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                        if(StringUtils.isBlank(repositoryDefinition.getRepositoryClass())) {
                            continue;
                        }

                        PhpClass phpClass = PhpElementsUtil.getClassInsideNamespaceScope(project, key, repositoryDefinition.getRepositoryClass());
                        if(phpClass != null) {
                            String presentableFQN = phpClass.getPresentableFQN();
                            if(!repositoryMap.containsKey(presentableFQN)) {
                                repositoryMap.put(presentableFQN, new HashSet<>());
                            }

                            repositoryMap.get(presentableFQN).add(key);
                        }
                    }
                }

                return CachedValueProvider.Result.create(repositoryMap, FileIndexCaches.getModificationTrackerForIndexId(project, DoctrineMetadataFileStubIndex.KEY));
            },
            false
        );

        repositoryClass = StringUtils.stripStart(repositoryClass,"\\");
        if(!cache.containsKey(repositoryClass)) {
            return Collections.emptyList();
        }

        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (String s : cache.get(repositoryClass)) {
            virtualFiles.addAll(
                FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, s, GlobalSearchScope.allScope(project))
            );
        }

        return virtualFiles;
    }

    /**
     * Find metadata model in which the given repository class is used
     * eg "@ORM\Entity(repositoryClass="FOOBAR")", xml or yaml
     */
    public static Collection<DoctrineModelInterface> findMetadataModelForRepositoryClass(final Project project, String repositoryClass) {
        repositoryClass = StringUtils.stripStart(repositoryClass,"\\");

        Collection<DoctrineModelInterface> models = new ArrayList<>();

        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            for (DoctrineModelInterface repositoryDefinition : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                String myRepositoryClass = repositoryDefinition.getRepositoryClass();
                if(StringUtils.isBlank(myRepositoryClass) ||
                    !repositoryClass.equalsIgnoreCase(StringUtils.stripStart(myRepositoryClass, "\\"))) {
                    continue;
                }

                models.add(repositoryDefinition);
            }
        }

        return models;
    }

    /**
     * Returns cached pairs of (tableName, classFqn) from the Doctrine metadata index.
     */
    public static Collection<Pair<String, String>> getTables(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            DOCTRINE_TABLES_CACHE,
            () -> {
                Collection<Pair<String, String>> pairs = new ArrayList<>();
                FileBasedIndex.getInstance().processAllKeys(DoctrineMetadataFileStubIndex.KEY, key -> {
                    for (DoctrineModelSerializable model : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                        String tableName = model.getTableName();
                        if (tableName != null) {
                            pairs.add(new Pair<>(tableName, model.getClassName()));
                        }
                    }
                    return true;
                }, project);
                return CachedValueProvider.Result.create(pairs, FileIndexCaches.getModificationTrackerForIndexId(project, DoctrineMetadataFileStubIndex.KEY));
            },
            false
        );
    }

    /**
     * Find Doctrine entity PHP classes for the given database table name.
     * Checks explicit table name mappings from the index, then guessing from the class.
     */
    public static Collection<PhpClass> findEntityByTableName(Project project, String tableName) {
        String normalizedTableName = tableName.trim().toLowerCase();
        Collection<PhpClass> navigatables = new HashSet<>();

        // First: use the index for explicit table name mappings (fast, cached)
        for (Pair<String, String> pair : getTables(project)) {
            if (!pair.getFirst().trim().toLowerCase().equals(normalizedTableName)) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, pair.getSecond());
            if (phpClass == null) {
                continue;
            }

            navigatables.add(phpClass);
        }

        if (!navigatables.isEmpty()) {
            return new ArrayList<>(navigatables);
        }

        // Fallback: guess table name from class short name (snake_case + optional plural)
        for (DoctrineModel model : EntityHelper.getModelClasses(project)) {
            PhpClass phpClass = model.getPhpClass();

            String shortName = phpClass.getName();
            if (shortName.isBlank()) {
                continue;
            }

            String singular = underscore(shortName);
            if (singular.isBlank()) {
                continue;
            }

            String plural = StringUtil.pluralize(singular);
            if (singular.equalsIgnoreCase(normalizedTableName) || plural.equalsIgnoreCase(normalizedTableName)) {
                navigatables.add(phpClass);
            }
        }

        return new ArrayList<>(navigatables);
    }

    @Nullable
    public static DoctrineMetadataModel getMetadataByTable(Project project, String tableName) {

        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile == null) {
                    continue;
                }

                DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, key);

                for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                    DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                    if(metadata == null) {
                        continue;
                    }

                    String table = metadata.getTable();
                    if(tableName.equals(table)) {
                        return metadata;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param className fully qualified class name (preferred), with or without leading backslash
     */
    @Nullable
    public static DoctrineMetadataModel getModelFields(Project project, String className) {
        for (VirtualFile file : findMetadataFiles(project, className)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if(psiFile == null) {
                continue;
            }

            DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, className);
            for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                if(metadata != null) {
                    return metadata;
                }
            }
        }

        return null;
    }

    public static Collection<PhpClass> getModels(Project project) {

        Collection<PhpClass> phpClasses = new ArrayList<>();
        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, key);
            if(classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    @Nullable
    public static DoctrineManagerEnum findManagerByScope(PsiElement psiElement) {
        String name = psiElement.getContainingFile().getName();
        Matcher matcher = Pattern.compile(".(mongodb|couchdb|orm|document|odm).(xml|yaml|yml)$", Pattern.CASE_INSENSITIVE).matcher(name);

        if(matcher.find()) {
            DoctrineManagerEnum managerEnum = DoctrineManagerEnum.getEnumFromString(matcher.group(1));
            if(managerEnum != null) {
                return managerEnum;
            }
        }

        // @TODO: implement psiElement position scope
        return null;
    }

    @Nullable
    public static String findModelNameInScope(PsiElement psiElement) {

        if(psiElement.getLanguage().equals(XMLLanguage.INSTANCE)) {
            XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class, false);
            while (xmlTag != null) {
                String tagName = xmlTag.getName();
                boolean isModelTag = tagName.equals("entity")
                    || tagName.equals("document")
                    || tagName.equals("embedded-document")
                    || tagName.equals("embeddable")
                    || tagName.equals("embedded") && xmlTag.getParentTag() != null && xmlTag.getParentTag().getName().matches(DoctrineMetadataPattern.DOCTRINE_MAPPING);

                String name = isModelTag ? xmlTag.getAttributeValue("name") : null;
                if(StringUtils.isNotBlank(name)) {
                    return name;
                }

                xmlTag = PsiTreeUtil.getParentOfType(xmlTag, XmlTag.class, true);
            }
        } else if(psiElement.getContainingFile() instanceof YAMLFile) {
            PsiFile psiFile = psiElement.getContainingFile();
            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument != null) {
                YAMLValue topLevelValue = yamlDocument.getTopLevelValue();
                if(topLevelValue instanceof YAMLMapping) {
                    YAMLKeyValue firstChild = PsiTreeUtil.findChildOfType(topLevelValue, YAMLKeyValue.class);
                    if(firstChild != null) {
                        String keyText = firstChild.getKeyText();
                        if(StringUtils.isNotBlank(keyText)) {
                            return keyText;
                        }
                    }
                }
            }
        }

        // @TODO: yml scope

        return null;
    }

    public static Collection<PhpClass> getClassInsideScope(PsiElement psiElement, String originValue) {
        Collection<PhpClass> classesInterface = new ArrayList<>();
        String modelNameInScope = DoctrineMetadataUtil.findModelNameInScope(psiElement);
        if(modelNameInScope != null) {
            PhpClass classInsideNamespaceScope = PhpElementsUtil.getClassInsideNamespaceScope(psiElement.getProject(), modelNameInScope, originValue);
            if(classInsideNamespaceScope != null) {
                classesInterface = Collections.singletonList(classInsideNamespaceScope);
            }
        } else {
            classesInterface = PhpElementsUtil.getClassesInterface(psiElement.getProject(), originValue);
        }
        // @TODO: multi classes
        return classesInterface;
    }
}
