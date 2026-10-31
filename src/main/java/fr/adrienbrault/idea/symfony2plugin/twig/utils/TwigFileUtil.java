package fr.adrienbrault.idea.symfony2plugin.twig.utils;

import com.jetbrains.twig.TwigFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileUtil {
    /**
     * Visit parent Twig files eg on "embed" tag and provide all files in this path until root file
     */
    public static Collection<VirtualFile> collectParentFiles(boolean includeSelf, Collection<PsiFile> psiFiles) {
        return collectParentFiles(includeSelf, psiFiles.toArray(new PsiFile[0]));
    }

    /**
     * Visit parent Twig files eg on "embed" tag and provide all files in this path until root file
     */
    public static Collection<VirtualFile> collectParentFiles(boolean includeSelf, PsiFile... psiFiles) {
        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (PsiFile psiFile : psiFiles) {
            VirtualFile sourceFile = psiFile.getVirtualFile();
            if(includeSelf) {
                virtualFiles.add(sourceFile);
            }

            visitParentFiles(psiFile, 0, virtualFiles);
        }

        return virtualFiles;
    }

    private static void visitParentFiles(PsiFile file, int depth, Collection<VirtualFile> virtualFiles) {
        // limit recursive calls
        if(depth++ > 20) {
            return;
        }

        // secure loading
        VirtualFile virtualFile1 = file.getVirtualFile();
        if (virtualFile1 == null) {
            return;
        }

        Set<VirtualFile> myVirtualFiles = new HashSet<>();
        Set<String> templates = new HashSet<>();

        FileBasedIndex.getInstance()
            .getValues(TwigBlockIndexExtension.KEY, "use", GlobalSearchScope.fileScope(file))
            .forEach(templates::addAll);

        FileBasedIndex.getInstance()
            .getFileData(TwigExtendsStubIndex.KEY, virtualFile1, file.getProject())
            .forEach((templateName, aVoid) -> templates.add(templateName));

        for (String template : templates) {
            for (VirtualFile virtualFile : TwigUtil.getTemplateFiles(file.getProject(), template)) {
                if (!virtualFiles.contains(virtualFile)) {
                    myVirtualFiles.add(virtualFile);
                    virtualFiles.add(virtualFile);
                }
            }
        }

        // visit files in this scope
        for(VirtualFile virtualFile : myVirtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(file.getProject()).findFile(virtualFile);
            if(psiFile instanceof TwigFile) {
                visitParentFiles(psiFile, depth, virtualFiles);
            }
        }
    }
}
