package fr.adrienbrault.idea.symfony2plugin.twig.loader;

import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileOverwritesLazyLoader {
    private final Project project;

    private final Map<VirtualFile, Collection<VirtualFile>> fileScope = new HashMap<>();

    private final Map<VirtualFile, Collection<VirtualFile>> selfScope = new HashMap<>();

    public FileOverwritesLazyLoader(Project project) {
        this.project = project;
    }

    public Collection<VirtualFile> getFiles(boolean includeSelf, Collection<VirtualFile> virtualFiles) {
        Map<VirtualFile, Collection<VirtualFile>> target;

        if(includeSelf) {
            // {{ block("foo") }}
            target = selfScope;
        } else {
            target = fileScope;
        }

        Collection<VirtualFile> virtualFileCollection = new HashSet<>();

        for (VirtualFile virtualFile : virtualFiles) {
            if(!target.containsKey(virtualFile)) {
                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if(file != null) {
                    target.put(virtualFile, TwigFileUtil.collectParentFiles(includeSelf, file));
                } else {
                    target.put(virtualFile, Collections.emptySet());
                }
            }

            virtualFileCollection.addAll(target.get(virtualFile));
        }

        return virtualFileCollection;
    }
}
