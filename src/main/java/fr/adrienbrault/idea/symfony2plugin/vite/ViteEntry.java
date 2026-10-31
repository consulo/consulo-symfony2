package fr.adrienbrault.idea.symfony2plugin.vite;

import consulo.language.psi.PsiElement;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Consulo port of the Kotlin data class
 */
public final class ViteEntry {
    private final String myName;
    private final @Nullable String myTargetPath;
    private final VirtualFile myConfigFile;
    private final @Nullable PsiElement myPsiElement;

    public ViteEntry(String name, @Nullable String targetPath, VirtualFile configFile, @Nullable PsiElement psiElement) {
        myName = name;
        myTargetPath = targetPath;
        myConfigFile = configFile;
        myPsiElement = psiElement;
    }

    public String getName() {
        return myName;
    }

    public @Nullable String getTargetPath() {
        return myTargetPath;
    }

    public VirtualFile getConfigFile() {
        return myConfigFile;
    }

    public @Nullable PsiElement getPsiElement() {
        return myPsiElement;
    }

    public String component1() {
        return myName;
    }

    public @Nullable String component2() {
        return myTargetPath;
    }

    public VirtualFile component3() {
        return myConfigFile;
    }

    public @Nullable PsiElement component4() {
        return myPsiElement;
    }

    public ViteEntry copy(String name, @Nullable String targetPath, VirtualFile configFile, @Nullable PsiElement psiElement) {
        return new ViteEntry(name, targetPath, configFile, psiElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViteEntry that)) {
            return false;
        }
        return Objects.equals(myName, that.myName)
            && Objects.equals(myTargetPath, that.myTargetPath)
            && Objects.equals(myConfigFile, that.myConfigFile)
            && Objects.equals(myPsiElement, that.myPsiElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myTargetPath, myConfigFile, myPsiElement);
    }

    @Override
    public String toString() {
        return "ViteEntry(name=" + myName + ", targetPath=" + myTargetPath + ", configFile=" + myConfigFile + ", psiElement=" + myPsiElement + ")";
    }
}
