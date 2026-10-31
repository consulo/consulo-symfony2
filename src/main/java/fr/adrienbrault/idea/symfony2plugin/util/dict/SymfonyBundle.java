package fr.adrienbrault.idea.symfony2plugin.util.dict;

import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundle {
    final private PhpClass phpClass;

    final private String phpClassName;

    private final String phpClassNameNamespaceName;

    public PhpClass getPhpClass() {
        return this.phpClass;
    }

    public String getNamespaceName() {
        return this.phpClass.getNamespaceName();
    }

    public String getName() {
        return this.phpClassName;
    }

    public SymfonyBundle(PhpClass phpClass) {
        this.phpClass = phpClass;

        this.phpClassName = this.phpClass.getName();
        this.phpClassNameNamespaceName = this.phpClass.getNamespaceName();
    }

    public @Nullable PsiDirectory getDirectory() {
        if(null == this.phpClass.getContainingFile()) {
            return null;
        }

        return this.phpClass.getContainingFile().getContainingDirectory();
    }

    public @Nullable PsiDirectory getSubDirectory(String... names) {
        PsiDirectory currentDir = this.getDirectory();
        if(null == currentDir) {
            return null;
        }

        for(String name: names) {
            currentDir = currentDir.findSubdirectory(name);
            if(null == currentDir) {
                return null;
            }
        }

        return currentDir;
    }

    public @Nullable String getRelativePath(VirtualFile file) {
        PsiDirectory currentDir = this.getDirectory();

        if(null == currentDir) {
          return null;
        }

        return VirtualFileUtil.getRelativePath(file, currentDir.getVirtualFile(), '/');
    }

    public boolean isInBundle(PhpClass phpClass) {
        return phpClass.getNamespaceName().startsWith(this.phpClassNameNamespaceName);
    }

    public boolean isInBundle(PsiFile psiFile) {
        return isInBundle(psiFile.getVirtualFile());
    }

    public boolean isInBundle(@Nullable VirtualFile virtualFile) {
        if(virtualFile == null) {
            return false;
        }

        PsiDirectory psiDirectory =  this.getDirectory();
        return psiDirectory != null && VirtualFileUtil.isAncestor(psiDirectory.getVirtualFile(), virtualFile, false);
    }

    public @Nullable VirtualFile getRelative(String path) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

       return VirtualFileUtil.findRelativeFile(virtualDirectory.getVirtualFile(), path.split("/"));
    }

    public @Nullable String getRelative(VirtualFile virtualFile) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

        return VirtualFileUtil.getRelativePath(virtualFile, virtualDirectory.getVirtualFile(), '/');
    }

    public @Nullable String getFileShortcut(BundleFile bundleFile) {
        String relativePath = this.getRelative(bundleFile.getVirtualFile());
        if(relativePath == null) {
            return null;
        }

        return "@" + this.getName() + "/" + relativePath;
    }

    public boolean isTestBundle() {
        PsiDirectory directory = this.getDirectory();
        if(directory == null) {
            return false;
        }

        // @TODO: filter vendor, src before?
        return directory.getVirtualFile().toString().contains("/Tests/");
    }

    public @Nullable String getParentBundleName() {
        return PhpElementsUtil.getMethodReturnAsString(this.getPhpClass(), "getParent");
    }

    /**
     * Returns all possible template directories for this bundle.
     * Supports both legacy (Resources/views) and new Symfony 4.4+ (templates) structures.
     */
    public Collection<PsiDirectory> getTemplateDirectories() {
        Collection<PsiDirectory> directories = new ArrayList<>();

        PsiDirectory legacy = getSubDirectory("Resources", "views");
        if (legacy != null) {
            directories.add(legacy);
        }

        PsiDirectory modern = getSubDirectory("templates");
        if (modern != null) {
            directories.add(modern);
        }

        return directories;
    }

}