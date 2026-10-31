package fr.adrienbrault.idea.symfony2plugin.util.resource;

import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.application.util.NotNullLazyValue;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.stub.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FileResourcesIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;
import fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceUtil {
    /**
     * chars that trigger a glob resolving on symfony
     * extracted from: \Symfony\Component\Config\Loader\FileLoader::import
     */
    public static final String[] GLOB_DETECTION_CHARS = {"*", "?", "{", "["};

    /**
     * Search for files refers to given file
     */
    public static boolean hasFileResources(Project project, PsiFile psiFile) {
        return LanguageCachedValueUtil.getCachedValue(
            psiFile,
            () -> {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile == null) {
                    return CachedValueProvider.Result.create(Boolean.FALSE, FileIndexCaches.getModificationTrackerForIndexId(project, FileResourcesIndex.KEY));
                }

                final Boolean[] aFalse = {Boolean.FALSE};

                visitFileResources(project, virtualFile, pair -> {
                    aFalse[0] = Boolean.TRUE;
                    return true;
                });

                return CachedValueProvider.Result.create(aFalse[0], FileIndexCaches.getModificationTrackerForIndexId(project, FileResourcesIndex.KEY));
            }
        );
    }

    /**
     * Search for files refers to given file
     */
    public static Collection<Pair<VirtualFile, String>> getFileResources(Project project, VirtualFile virtualFile) {
        Collection<Pair<VirtualFile, String>> files = new ArrayList<>();

        visitFileResources(project, virtualFile, pair -> {
            files.add(Pair.create(pair.getFirst(), pair.getSecond()));
            return false;
        });

        return files;
    }

    /**
     * Search for files refers to given file
     */
    public static void visitFileResources(Project project, VirtualFile virtualFile, Function<Pair<VirtualFile, String>, Boolean> consumer) {
        Set<VirtualFile> files = new HashSet<>();
        for (String resource : IndexUtil.getAllKeysForProject(FileResourcesIndex.KEY, project)) {
            files.addAll(FileBasedIndex.getInstance().getContainingFiles(FileResourcesIndex.KEY, resource, GlobalSearchScope.allScope(project)));
        }

        for (VirtualFile containingFile : files) {
            for (FileResource fileResourceContext : FileBasedIndex.getInstance().getFileData(FileResourcesIndex.KEY, containingFile, project).values()) {
                String resource = fileResourceContext.getResource();
                if (resource == null) {
                    continue;
                }

                VirtualFile directory = containingFile.getParent();
                if (directory == null) {
                    continue;
                }

                String resourceResolved = resource;

                if (resource.startsWith("@")) {
                    String replace = resource.replace("\\", "/");
                    int i = replace.indexOf("/");

                    boolean resolved = false;

                    if (i > 2 || i == -1) {
                        if (i == -1) {
                            i = resource.length();
                        }

                        String substring = resource.substring(1, i);
                        Collection<SymfonyBundle> bundle = new SymfonyBundleUtil(project).getBundle(substring);

                        for (SymfonyBundle symfonyBundle : bundle) {
                            PsiDirectory directory1 = symfonyBundle.getDirectory();
                            if (directory1 == null) {
                                continue;
                            }

                            resourceResolved = resource.substring(replace.contains("/") ? i + 1 : replace.length());
                            directory = directory1.getVirtualFile();

                            resolved = true;

                            break;
                        }
                    }

                    if (!resolved) {
                        continue;
                    }
                }

                // '../src/{Entity}'
                // '../src/*Controller.php'
                if (Arrays.stream(GLOB_DETECTION_CHARS).anyMatch(resource::contains)) {
                    String path = directory.getPath();

                    // nested types not support by java glob implementation so just catch the exception: "../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php,Service/{IspConfiguration,DataCollection}}"
                    try {
                        String s1 = Paths.get(path + File.separatorChar + StringUtils.stripStart(resourceResolved, "\\/")).normalize().toString();
                        String syntaxAndPattern = "glob:" + s1;
                        if (FileSystems.getDefault().getPathMatcher(syntaxAndPattern).matches(Paths.get(virtualFile.getPath()))) {
                            if (consumer.apply(new Pair<>(containingFile, resource))) {
                                return;
                            }
                        }
                    } catch (PatternSyntaxException | InvalidPathException ignored) {
                    }

                    continue;
                }

                // '../src/FooController.php'
                VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(directory, resourceResolved.replace("\\", "/").split("/"));
                if (relativeFile != null && relativeFile.equals(virtualFile)) {
                    if (consumer.apply(new Pair<>(containingFile, resource))) {
                        return;
                    }
                }

                // '..src/Controller'
                if (fileResourceContext.getContextType() == FileResourceContextTypeEnum.ROUTE) {
                    if (StringUtils.isNotBlank(resourceResolved)) {
                        directory = VirtualFileUtil.findRelativeFile(directory, resourceResolved.replace("\\", "/").split("/"));
                        if (directory == null) {
                            continue;
                        }
                    }

                    if (VirtualFileUtil.isAncestor(directory, virtualFile, false)) {
                        if (consumer.apply(new Pair<>(containingFile, resource))) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static @Nullable RelatedItemLineMarkerInfo<PsiElement> getFileImplementsLineMarker(PsiFile psiFile) {
        final Project project = psiFile.getProject();

        VirtualFile virtualFile = psiFile.getVirtualFile();
        if(virtualFile == null) {
            return null;
        }

        if (hasFileResources(project, psiFile)) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS)
                .setTargets(NotNullLazyValue.createValue(new FileResourceNotNullLazyValue(project, virtualFile)))
                .setTooltipText("Navigate to resource");

            return FileLineMarkerUtil.createLineMarkerInfo(builder, psiFile);
        }

        return null;
    }

    /**
     * Gives targets to files on Bundle locate syntax. "@FooBundle/.../foo.yml"
     */
    public static Collection<PsiFile> getFileResourceTargetsInBundleScope(Project project, String content) {

        // min validation "@FooBundle/foo.yml"
        if(!content.startsWith("@") || !content.contains("/")) {
            return Collections.emptyList();
        }

        String bundleName = content.substring(1, content.indexOf("/"));

        Collection<PsiFile> targets = new HashSet<>();

        for (SymfonyBundle bundle : new SymfonyBundleUtil(project).getBundle(bundleName)) {
            String path = content.substring(content.indexOf("/") + 1);
            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, bundle.getRelative(path));

            if(psiFile != null) {
                targets.add(psiFile);
            }
        }

        return targets;
    }

    /**
     * resource: "@AppBundle/Controller/"
     */
    public static Collection<PsiElement> getFileResourceTargetsInBundleDirectory(Project project, String content) {
        // min validation "@FooBundle/foo.yml"
        if(!content.startsWith("@")) {
            return Collections.emptyList();
        }

        content = content.replace("/", "\\");
        if(!content.contains("\\")) {
            return Collections.emptyList();
        }

        String bundleName = content.substring(1, content.indexOf("\\"));

        if(new SymfonyBundleUtil(project).getBundle(bundleName).isEmpty()) {
            return Collections.emptyList();
        }

        // support double backslashes
        content = content.replaceAll("\\\\+", "\\\\");
        String namespaceName = "\\" + StringUtils.strip(content.substring(1), "\\");
        return new ArrayList<>(PhpIndexUtil.getPhpClassInsideNamespace(
            project, namespaceName
        ));
    }

    /**
     * Gives targets to files which relative to current file directory
     */
    public static Collection<PsiFile> getFileResourceTargetsInDirectoryScope(PsiFile psiFile, String content) {

        // bundle scope
        if(content.startsWith("@")) {
            return Collections.emptyList();
        }

        PsiDirectory containingDirectory = psiFile.getContainingDirectory();
        if(containingDirectory == null) {
            return Collections.emptyList();
        }

        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(content, containingDirectory.getVirtualFile());
        if(relativeFile == null) {
            return Collections.emptyList();
        }

        Set<PsiFile> psiFiles = new HashSet<>();
        Project project = psiFile.getProject();

        if (relativeFile.isDirectory()) {
            String path = relativeFile.getPath();
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/*.php");

            Set<String> files = new HashSet<>();
            try {
                Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        if (pathMatcher.matches(path)) {
                            files.add(path.toString());
                        }
                        return files.size() < 200 ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }

            Set<PsiFile> collect = files.stream()
                .map(s -> VirtualFileUtil.findFileByIoFile(new File(s), false))
                .filter(Objects::nonNull)
                .map(virtualFile -> PsiElementUtils.virtualFileToPsiFile(project, virtualFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            psiFiles.addAll(collect);
        } else {
            PsiFile targetFile = PsiElementUtils.virtualFileToPsiFile(project, relativeFile);
            if(targetFile != null) {
                psiFiles.add(targetFile);
            }
        }

        return psiFiles;
    }

    /**
     * Resolves a "resource" path to its target PSI elements (files, classes, directories).
     * Supports bundle scope ("@Bundle/..."), glob patterns ("../src/*.php"), and relative paths.
     */
    public static Collection<PsiElement> getFileResourceTargets(Project project, PsiFile containingFile, String resourcePath) {
        if (StringUtils.isBlank(resourcePath)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new HashSet<>();

        if (Arrays.stream(GLOB_DETECTION_CHARS).anyMatch(resourcePath::contains)) {
            VirtualFile containingVirtualFile = containingFile.getVirtualFile();
            if (containingVirtualFile != null) {
                targets.addAll(PsiElementUtils.convertVirtualFilesToPsiFiles(project, getFilesForResources(project, containingVirtualFile, resourcePath)));
            }
        } else {
            targets.addAll(getFileResourceTargetsInDirectoryScope(containingFile, resourcePath));
        }

        if (resourcePath.startsWith("@")) {
            targets.addAll(getFileResourceTargetsInBundleScope(project, resourcePath));

            if (targets.isEmpty()) {
                targets.addAll(getFileResourceTargetsInBundleDirectory(project, resourcePath));
            }
        }

        return targets;
    }

    /**
     * Fast path check whether a "resource" path resolves to any targets,
     * without collecting the full result set.
     */
    public static boolean hasFileResourceTargets(Project project, PsiFile containingFile, String resourcePath) {
        if (StringUtils.isBlank(resourcePath)) {
            return false;
        }

        if (resourcePath.startsWith("@")) {
            if (!getFileResourceTargetsInBundleScope(project, resourcePath).isEmpty()) {
                return true;
            }

            return !getFileResourceTargetsInBundleDirectory(project, resourcePath).isEmpty();
        }

        if (Arrays.stream(GLOB_DETECTION_CHARS).anyMatch(resourcePath::contains)) {
            VirtualFile containingVirtualFile = containingFile.getVirtualFile();
            return containingVirtualFile != null && !getFilesForResources(project, containingVirtualFile, resourcePath).isEmpty();
        }

        return !getFileResourceTargetsInDirectoryScope(containingFile, resourcePath).isEmpty();
    }

    private record FileResourceNotNullLazyValue(Project project, VirtualFile virtualFile) implements Supplier<Collection<? extends PsiElement>> {
        private FileResourceNotNullLazyValue(Project project, VirtualFile virtualFile) {
            this.project = project;
            this.virtualFile = virtualFile;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            Collection<PsiElement> psiElements = new HashSet<>();

            for (Pair<VirtualFile, String> pair : getFileResources(project, virtualFile)) {
                PsiFile psiFile1 = PsiElementUtils.virtualFileToPsiFile(project, pair.getFirst());
                if (psiFile1 == null) {
                    continue;
                }

                FileResourceVisitorUtil.visitFile(psiFile1, fileResourceConsumer -> {
                    if (fileResourceConsumer.getResource().equalsIgnoreCase(pair.getSecond())) {
                        psiElements.add(fileResourceConsumer.getPsiElement());
                    }
                });
            }

            return psiElements;
        }
    }

    /**
     * Split given "resource" path to get the root path and its pattern
     *
     * @param resourcePath "../src/{Entity,Foobar}/"
     * @return "../src", "{Entity,Foobar}"
     */
    public static Pair<String, String> getGlobalPatternDirectory(String resourcePath) {
        String[] split = resourcePath.split("/");
        List<String> path = new ArrayList<>();

        for (int i = 0; i < split.length; i++) {
            String s1 = split[i];
            if (Stream.of("$", "*", "[", "]", "|", "(", ")", "?", "{", "}").anyMatch(s1::contains)) {
                String join = String.join("/", Arrays.copyOfRange(split, i, split.length));

                if (resourcePath.endsWith("/") && !join.endsWith("/")) {
                    join += "/";
                }

                return new Pair<>(String.join("/", path), join);
            }

            path.add(s1);
        }

        String join = String.join("/", path);
        if (resourcePath.endsWith("/") && !join.endsWith("/")) {
            join += "/";
        }

        return new Pair<>(join, null);
    }

    /**
     * Resolve a relative resource glob against a concrete base path without ever passing the glob segment to {@link Path#of(String, String...)}.
     * This keeps Windows and WSL paths safe because wildcard characters are only applied after the non-glob root has been normalized.
     */
    public static Pair<String, String> getGlobalPatternDirectory(Path parentPath, String resourcePath) {
        Pair<String, String> globalPatternDirectory = getGlobalPatternDirectory(
            StringUtils.stripStart(resourcePath.replace("\\", "/"), "/")
        );

        String rootPath = globalPatternDirectory.getFirst();
        Path resolvedPath = parentPath.resolve(rootPath.isEmpty() ? "." : rootPath).normalize();

        return Pair.create(resolvedPath.toString(), globalPatternDirectory.getSecond());
    }

    /**
     * controllers:
     *    resource: '../../src/Controller/'
     * controllers:
     *     resource:
     *         path: ../src/Controller/
     */
    public static @Nullable String getResourcePath(YAMLKeyValue yamlKeyValue) {
        YAMLKeyValue resourceYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, "resource");
        if (resourceYamlKeyValue == null) {
            return null;
        }

        YAMLValue value = resourceYamlKeyValue.getValue();

        String resource = null;
        if (value instanceof YAMLScalar) {
            resource = ((YAMLScalar) value).getTextValue();
        } else if (value instanceof YAMLMapping) {
            resource = YamlHelper.getYamlKeyValueAsString((YAMLMapping) value, "path");
        }

        if (resource == null || resource.isBlank()) {
            return null;
        }

        return resource;
    }

    public static NavigationGutterIconBuilder<PsiElement> getNavigationGutterForRouteAnnotationResources(Project project, VirtualFile virtualFile, String resource) {
        return NavigationGutterIconBuilder.create(PlatformIconGroup.modulesSourceroot())
            .setTargets(NotNullLazyValue.createValue(() -> {
                String r = resource;

                // try to resolve the following pattern; all are recursive search for php
                // ../../Core/**/**/Controller/*Controller.php
                // ../src/Controller/
                // ../src/Kernel.php
                if (resource.endsWith("/") || resource.endsWith("\\")) {
                    r += "**.php";
                } else if (Arrays.stream(FileResourceUtil.GLOB_DETECTION_CHARS).noneMatch(r::contains) && !resource.toLowerCase().endsWith(".php")) {
                    r += "**.php";
                }

                Collection<VirtualFile> filesForResources = FileResourceUtil.getFilesForResources(project, virtualFile, r)
                    .stream()
                    .filter(virtualFile1 -> "php".equalsIgnoreCase(virtualFile1.getExtension())).collect(Collectors.toSet());

                return PsiElementUtils.convertVirtualFilesToPsiFiles(project, filesForResources);
            }))
            .setTooltipText("Navigate to matching files");
    }

    /**
     * Find a file based on "glob" and its content path.
     *
     * @param virtualFile Current scope, parent (directory) is taking for resolving root dirctory to glob operation
     * @param glob "@FooBundle/foo.xml", "src/foo.xml", "src/.../src.xml"
     */
    public static Collection<VirtualFile> getFilesForResources(Project project, VirtualFile virtualFile, String glob) {
        VirtualFile parent = virtualFile.getParent();
        if (parent == null) {
            return Collections.emptyList();
        }

        if (glob.startsWith("@")) {
            String replace = glob.replace("\\", "/");
            int i = replace.indexOf("/");

            boolean resolved = false;

            if (i > 2 || i == -1) {
                if (i == -1) {
                    i = glob.length();
                }

                String substring = glob.substring(1, i);


                for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundle(substring)) {
                    PsiDirectory directory1 = symfonyBundle.getDirectory();
                    if (directory1 == null) {
                        continue;
                    }

                    glob = glob.substring(replace.contains("/") ? i + 1 : replace.length());
                    parent = directory1.getVirtualFile();

                    resolved = true;

                    break;
                }

            }

            if (!resolved) {
                return Collections.emptyList();
            }

        }

        Pair<String, String> globalPatternDirectory = getGlobalPatternDirectory(Path.of(parent.getPath()), glob);
        Path rootPath = Path.of(globalPatternDirectory.getFirst());

        Collection<VirtualFile> files = new HashSet<>();
        if (globalPatternDirectory.getSecond() == null) {
            VirtualFile target = LocalFileSystem.getInstance().findFileByNioFile(rootPath);
            if (target != null) {
                files.add(target);
            }

            return files;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath, globalPatternDirectory.getSecond())) {
            for (Path file : directoryStream) {
                VirtualFile target = LocalFileSystem.getInstance().findFileByNioFile(file);
                if (target != null) {
                    files.add(target);
                }
            }
        } catch (PatternSyntaxException | IOException ignored) {
        }

        return files;
    }
}
