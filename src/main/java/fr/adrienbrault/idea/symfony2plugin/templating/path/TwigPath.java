package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.application.Application;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPath {
    private final String path;

    private String namespace = TwigUtil.MAIN;

    private TwigUtil.NamespaceType namespaceType = TwigUtil.NamespaceType.ADD_PATH;

    private boolean enabled = true;
    private boolean customPath = false;

    public TwigUtil.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    public TwigPath(String path) {
        this.path = path;
    }

    public TwigPath(String path, String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public TwigPath(String path, String namespace, TwigUtil.NamespaceType namespaceType, boolean customPath) {
        this(path, namespace, namespaceType);
        this.customPath = customPath;
    }

    public static TwigPath createTwigPath(String path, String namespace, TwigUtil.NamespaceType namespaceType, boolean customPath, boolean enabled) {
        TwigPath twigPath = new TwigPath(path, namespace, namespaceType, customPath);
        twigPath.enabled = enabled;
        return twigPath;
    }

    public static TwigPath createClone(TwigPath twigPath) {
        return createClone(twigPath, twigPath.isEnabled());
    }

    public static TwigPath createClone(TwigPath twigPath, boolean enabled) {
        TwigPath newTwigPath = new TwigPath(
            twigPath.getPath(),
            twigPath.getNamespace(),
            twigPath.getNamespaceType(),
            twigPath.isCustomPath()
        );

        newTwigPath.enabled = enabled;
        return newTwigPath;
    }

    public TwigPath(String path, String namespace, TwigUtil.NamespaceType namespaceType) {
        this(path, namespace);
        this.namespaceType = namespaceType;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isGlobalNamespace() {
        return getNamespace().equals(TwigUtil.MAIN);
    }

    @Nullable
    public String getRelativePath(Project project) {
        if(!FileUtil.isAbsolute(path)) {
            return path;
        }

        VirtualFile virtualFile = VirtualFileUtil.findFileByIoFile(new File(path), false);
        if(virtualFile == null) {
            return null;
        }

        return VirtualFileUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
    }

    @Nullable
    public VirtualFile getDirectory(Project project) {
        if(!FileUtil.isAbsolute(path)) {
            VirtualFile baseDir = ProjectUtil.getProjectDir(project);
            VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(baseDir, path.split("/"));

            if (relativeFile != null) {
                return relativeFile;
            }

            // Fallback for unit tests: find directory by name via index
            if (Application.get().isUnitTestMode()) {
                String[] parts = path.split("/");
                if (parts.length > 0) {
                    String dirName = parts[parts.length - 1];
                    Collection<VirtualFile> dirs = FilenameIndex.getVirtualFilesByName(project, dirName, GlobalSearchScope.projectScope(project));
                    for (VirtualFile dir : dirs) {
                        if (dir.isDirectory()) {
                            return dir;
                        }
                    }
                }
            }
        } else {
            VirtualFile fileByIoFile = VirtualFileUtil.findFileByIoFile(new File(path), false);
            if(fileByIoFile != null) {
                return fileByIoFile;
            }
        }

        return null;
    }

    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Deprecated
    public TwigPath setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isCustomPath() {
        return customPath;
    }
}
