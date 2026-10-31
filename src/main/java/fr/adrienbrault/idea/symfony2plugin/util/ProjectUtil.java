package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

/**
 * All project related function
 */
public class ProjectUtil {
    /**
     * Single project usage is deprecated which wraps the single project resolving
     *
     * RootManager should be taken as is module based and allows more then one root; for now change all is too much
     * You should provide a file context, with this help we can search for in which root its given; see replacement function
     */
    public static VirtualFile getProjectDir(Project project) {
        return project.getBaseDir();
    }

    /**
     * This function should be use as a replaced, with given a context we know a possible project root
     */
    public static VirtualFile getProjectDir(PsiElement context) {
        return getProjectDir(context.getProject());
    }
}
