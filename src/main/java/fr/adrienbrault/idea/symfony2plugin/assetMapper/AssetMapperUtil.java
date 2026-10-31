package fr.adrienbrault.idea.symfony2plugin.assetMapper;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.AssetMapperModule;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.MappingFileEnum;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class AssetMapperUtil {
    public static final AssetMapperUtil INSTANCE = new AssetMapperUtil();

    private static final Key<CachedValue<List<AssetMapperModule>>> MAPPING_CACHE = Key.create("SYMFONY_ASSET_MAPPER_MAPPING_CACHE");

    private AssetMapperUtil() {
    }

    public static List<AssetMapperModule> getMappingFiles(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            MAPPING_CACHE,
            () -> CachedValueProvider.Result.create(
                getMappingFilesInner(project),
                PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
            ),
            false
        );
    }

    private static List<AssetMapperModule> getMappingFilesInner(Project project) {
        List<AssetMapperModule> modules = new ArrayList<>();
        Set<VirtualFile> files = new LinkedHashSet<>();

        VirtualFile importMap = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "importmap.php");
        if (importMap != null) {
            files.add(importMap);
        }

        VirtualFile installed = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "assets", "vendor", "installed.php");
        if (installed != null) {
            files.add(installed);
        }

        files.addAll(FilenameIndex.getVirtualFilesByName(project, "importmap.php", GlobalSearchScope.allScope(project)));
        for (VirtualFile file : FilenameIndex.getVirtualFilesByName(project, "installed.php", GlobalSearchScope.allScope(project))) {
            // composer
            VirtualFile parent = file.getParent();
            if (parent != null && "composer".equals(parent.getName())) {
                continue;
            }
            files.add(file);
        }

        for (VirtualFile file : files) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                continue;
            }

            for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(psiFile, PhpReturn.class)) {
                PsiElement argument = phpReturn.getArgument();
                if (argument instanceof ArrayCreationExpression arrayCreationExpression) {
                    for (Map.Entry<String, PsiElement> entry : PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(arrayCreationExpression).entrySet()) {
                        String key = entry.getKey();
                        PsiElement value = entry.getValue();

                        String path = null;
                        String url = null;
                        String version = null;
                        Boolean entrypoint = false;
                        String type = null;

                        if (value instanceof ArrayCreationExpression valueArray) {
                            path = PhpElementsUtil.getArrayValueString(valueArray, "path");
                            url = PhpElementsUtil.getArrayValueString(valueArray, "url");
                            version = PhpElementsUtil.getArrayValueString(valueArray, "version");
                            entrypoint = PhpElementsUtil.getArrayValueBool(valueArray, "entrypoint");
                            type = PhpElementsUtil.getArrayValueString(valueArray, "type");
                        }

                        modules.add(new AssetMapperModule(MappingFileEnum.fromString(file.getName()), file, key, path, url, version, entrypoint, type));
                    }
                }
            }
        }

        return modules;
    }

    private static Collection<VirtualFile> getModuleReferences(String module, List<AssetMapperModule> modules) {
        Set<VirtualFile> files = new HashSet<>();

        for (AssetMapperModule mappingFile : modules) {
            if (!mappingFile.key().equals(module)) {
                continue;
            }

            if (mappingFile.sourceType() == MappingFileEnum.IMPORTMAP) {
                // default project structure: "assets/vendor/*"
                VirtualFile parent = mappingFile.sourceFile().getParent();
                if (parent != null) {
                    if (mappingFile.path() != null) {
                        // simple path normalize: "./app.js"
                        String[] split = Arrays.stream(StringUtils.split(mappingFile.path(), "/"))
                            .filter(it -> !".".equals(it))
                            .toArray(String[]::new);

                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    }
                    else {
                        if ("css".equals(mappingFile.type())) {
                            String[] split = StringUtils.split("assets/vendor/" + mappingFile.key(), "/");
                            VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, split);
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        }
                        else if (mappingFile.key().contains("/")) {
                            String path;
                            if (mappingFile.key().startsWith("@") && mappingFile.key().split("/", -1).length == 2) {
                                path = mappingFile.key() + "/" + mappingFile.key().split("/", -1)[1] + ".index.js";
                            }
                            else {
                                path = mappingFile.key() + ".js";
                            }

                            String[] split = StringUtils.split("assets/vendor/" + path, "/");
                            VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, split);
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        }
                        else {
                            VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, "assets", "vendor", mappingFile.key(), mappingFile.key() + ".index.js");
                            if (relativeFile != null) {
                                files.add(relativeFile);
                                break;
                            }
                        }
                    }
                }
            }
            else if (mappingFile.sourceType() == MappingFileEnum.INSTALLED) {
                // fallback without project structure: every folder like "vendor/installed.php" => "vendor/bootstrap"
                VirtualFile parent = mappingFile.sourceFile().getParent();
                if (parent != null) {
                    if (mappingFile.key().endsWith("css")) {
                        String[] split = StringUtils.split(mappingFile.key(), "/");
                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    }
                    else if (mappingFile.key().contains("/")) {
                        String path;
                        if (mappingFile.key().startsWith("@") && mappingFile.key().split("/", -1).length == 2) {
                            path = mappingFile.key() + "/" + mappingFile.key().split("/", -1)[1] + ".index.js";
                        }
                        else {
                            path = mappingFile.key() + ".js";
                        }

                        String[] split = StringUtils.split(path, "/");
                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, split);
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    }
                    else {
                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(parent, mappingFile.key(), mappingFile.key() + ".index.js");
                        if (relativeFile != null) {
                            files.add(relativeFile);
                            break;
                        }
                    }
                }
            }
        }

        return files;
    }

    public static Collection<VirtualFile> getEntrypointModuleReferences(Project project, String module) {
        List<AssetMapperModule> collect = getEntrypointMappings(project).stream()
            .filter(it -> it.key().equals(module))
            .toList();

        // mapping targets
        Set<VirtualFile> files = new HashSet<>();
        for (AssetMapperModule it : collect) {
            files.add(it.sourceFile());
        }

        // mapping reference tags
        files.addAll(getModuleReferences(module, collect));

        return files;
    }

    private static List<AssetMapperModule> getEntrypointMappings(Project project) {
        return getMappingFiles(project).stream()
            .filter(it -> Boolean.TRUE.equals(it.entrypoint()))
            .toList();
    }

    public static Collection<LookupElement> getLookupElements(Project project) {
        return getLookupElements(getMappingFiles(project));
    }

    public static Collection<LookupElement> getEntrypointLookupElements(Project project) {
        return getLookupElements(getEntrypointMappings(project));
    }

    public static Collection<LookupElement> getLookupElements(List<AssetMapperModule> modules) {
        List<LookupElement> lookupElements = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (AssetMapperModule module : modules) {
            if (visited.contains(module.key())) {
                continue;
            }

            visited.add(module.key());

            LookupElementBuilder elementBuilder = LookupElementBuilder.create(module.key()).withIcon(Symfony2Icons.SYMFONY);
            String typeText = "";

            if (module.url() != null) {
                typeText = module.url();
            }
            else if (module.path() != null) {
                typeText = module.path();
            }

            if (module.version() != null) {
                if (!typeText.isBlank()) {
                    typeText = module.version() + " " + typeText;
                }
                else {
                    typeText = module.version();
                }
            }

            if (!typeText.isBlank()) {
                elementBuilder = elementBuilder.withTypeText(typeText);
            }

            lookupElements.add(elementBuilder);
        }

        return lookupElements;
    }
}
