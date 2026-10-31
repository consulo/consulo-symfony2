package fr.adrienbrault.idea.symfony2plugin.asset;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.webpack.SymfonyWebpackUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetDirectoryReader {

    public static AssetDirectoryReader INSTANCE = new  AssetDirectoryReader();

    final private boolean includeBundleDir;

    final private Collection<String> filterExtension = new HashSet<>();

    public AssetDirectoryReader() {
        includeBundleDir = false;
    }

    public AssetDirectoryReader(String[] filterExtension, boolean includeBundleDir) {
        this.includeBundleDir = includeBundleDir;
        this.filterExtension.addAll(Arrays.asList(filterExtension));
    }

    private static Collection<VirtualFile> getProjectAssetRoot(Project project) {
        Set<String> paths = new HashSet<>();

        // custom config
        String directoryToWeb = Settings.getInstance(project).directoryToWeb;
        if (StringUtils.isNotBlank(directoryToWeb)) {
            paths.add(directoryToWeb);
        }

        paths.add("public"); // latest Symfony structure
        paths.add("web"); // old Symfony structure

        // find asset mapper files
        for (VirtualFile file : FilenameIndex.getVirtualFilesByName(project, "installed.php", GlobalSearchScope.allScope(project))) {
            // composer
            VirtualFile parent = file.getParent();
            if (parent == null || !"vendor".equals(parent.getName())) {
                continue;
            }

            VirtualFile assetFolder = parent.getParent();
            if (assetFolder == null) {
                continue;
            }

            String relativePath = VirtualFileUtil.getRelativePath(assetFolder, ProjectUtil.getProjectDir(project), '/');
            if (relativePath != null) {
                paths.add(relativePath);
            }
        }

        return paths.stream()
            .map(path -> VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), path.split("/")))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Collection<AssetFile> getAssetFiles(Project project) {
        Collection<AssetFile> files = new ArrayList<>();

        Collection<VirtualFile> projectAssetRoots = getProjectAssetRoot(project);
        for (VirtualFile webDirectory : projectAssetRoots) {
            VirtualFileUtil.visitChildrenRecursively(webDirectory, new VirtualFileVisitor<VirtualFile>() {
                @Override
                public boolean visitFile(VirtualFile virtualFile) {
                    if(isValidFile(virtualFile)) {
                        files.add(new AssetFile(virtualFile, AssetEnum.Position.Web, webDirectory));
                    }
                    return super.visitFile(virtualFile);
                }
            });
        }

        for (VirtualFile projectManifestJsonFile : getProjectManifestJsonFiles(project, projectAssetRoots)) {
            SymfonyWebpackUtil.visitManifestJsonEntries(
                projectManifestJsonFile,
                pair -> files.add(AssetFile.createVirtualManifestEntry(projectManifestJsonFile, pair.getFirst()))
            );
        }

        if(!this.includeBundleDir) {
            return files;
        }

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(project);
        for(SymfonyBundle bundle : symfonyBundleUtil.getBundles()) {
            PsiDirectory bundleDirectory = bundle.getDirectory();
            if(null == bundleDirectory) {
                continue;
            }

            VirtualFile bundleDirectoryVirtual = bundleDirectory.getVirtualFile();
            VirtualFile resourceDirectory = VirtualFileUtil.findRelativeFile(bundleDirectoryVirtual, "Resources");

            if (null != resourceDirectory) {
                VirtualFileUtil.visitChildrenRecursively(resourceDirectory, new VirtualFileVisitor<VirtualFile>() {
                    @Override
                    public boolean visitFile(VirtualFile virtualFile) {
                        if(isValidFile(virtualFile)) {
                            files.add(new AssetFile(virtualFile, AssetEnum.Position.Bundle, bundleDirectoryVirtual, '@' + bundle.getName() + "/"));
                        }
                        return super.visitFile(virtualFile);
                    }
                });
            }
        }

        return files;
    }

    /**
     * '@SampleBundle/Resources/public/js/*'
     * 'assets/js/*'
     * 'assets/js/*.js'
     */
    public Collection<VirtualFile> resolveAssetFile(Project project, String filename) {
        String assetName = StringUtils.stripStart(filename.replace("\\", "/").replaceAll("/+", "/"), "/");

        // '@SampleBundle/Resources/public/js/foo,js'
        // TODO: '@SampleBundle/Resources/public/js/*'
        // TODO: '@SampleBundle/Resources/public/js/*.js'
        if(filename.startsWith("@")) {
            Collection<VirtualFile> files = new HashSet<>();

            int i = filename.indexOf("/");
            if(i > 0) {
                String relativeFilename = filename.substring(1, i);
                for (SymfonyBundle bundle : new SymfonyBundleUtil(project).getBundle(relativeFilename)) {
                    String assetPath = filename.substring(i + 1);

                    Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(assetPath);
                    if (!matcher.find()) {
                        VirtualFile relative = bundle.getRelative(assetPath);
                        if(relative != null) {
                            files.add(relative);
                        }
                    } else {
                        // "/*"
                        // "/*.js"
                        PsiDirectory directory = bundle.getDirectory();
                        if(directory != null) {
                            files.addAll(collectWildcardDirectories(matcher, directory.getVirtualFile()));
                        }
                    }
                }
            }

            return files;
        }

        Collection<VirtualFile> files = new HashSet<>();

        Collection<VirtualFile> projectAssetRoots = getProjectAssetRoot(project);
        for (VirtualFile webDirectory : projectAssetRoots) {
            Matcher matcher = Pattern.compile("^(.*[/\\\\])\\*([.\\w+]*)$").matcher(assetName);
            if (!matcher.find()) {
                VirtualFile assetFile = VirtualFileUtil.findRelativeFile(webDirectory, assetName.split("/"));
                if(assetFile != null) {
                    files.add(assetFile);
                }
            } else {
                // "/*"
                // "/*.js"
                files.addAll(collectWildcardDirectories(matcher, webDirectory));
            }
        }

        for (VirtualFile projectManifestJsonFile : getProjectManifestJsonFiles(project, projectAssetRoots)) {
            SymfonyWebpackUtil.visitManifestJsonEntries(
                projectManifestJsonFile,
                pair -> {
                    if (filename.equalsIgnoreCase(pair.getFirst())) {
                        files.add(projectManifestJsonFile);
                    }
                }
            );
        }

        return files;
    }

    private Collection<VirtualFile> collectWildcardDirectories(Matcher matcher, VirtualFile directory) {
        Collection<VirtualFile> files = new HashSet<>();

        String pathName = matcher.group(1);
        String fileExtension = !matcher.group(2).isEmpty() ? matcher.group(2) : null;

        pathName = StringUtils.stripEnd(pathName, "/");

        if(fileExtension == null) {
            // @TODO: filter files
            // 'assets/js/*'
            VirtualFile assetFile = VirtualFileUtil.findRelativeFile(directory, pathName.split("/"));
            if(assetFile != null) {
                files.add(assetFile);
            }
        } else {
            // @TODO: filter files
            // 'assets/js/*.js'
            VirtualFile assetFile = VirtualFileUtil.findRelativeFile(directory, pathName.split("/"));
            if(assetFile != null) {
                files.add(assetFile);
            }
        }

        return files;
    }

    private boolean isValidFile(VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            return false;
        }

        if(filterExtension.isEmpty()) {
            return true;
        }

        String extension = virtualFile.getExtension();
        return extension != null && this.filterExtension.contains(extension);
    }

    private Collection<VirtualFile> getProjectManifestJsonFiles(Project project, Collection<VirtualFile> webDirectories) {
        HashSet<VirtualFile> manifestFiles = new HashSet<>(
            FilenameIndex.getVirtualFilesByName(project, "manifest.json", GlobalSearchScope.allScope(project))
        );

        // for files which are ignored by indexing, resolve based on project web folder
        for (VirtualFile webDirectory : webDirectories) {
            VirtualFile fileByRelativePath = webDirectory.findFileByRelativePath("build/manifest.json");
            if (fileByRelativePath != null) {
                manifestFiles.add(fileByRelativePath);
            }
        }

        return manifestFiles;
    }
}
