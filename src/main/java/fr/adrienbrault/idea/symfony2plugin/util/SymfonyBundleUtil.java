package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleUtil {
    private final Project project;

    @Nullable
    private Collection<SymfonyBundle> symfonyBundles;

    public SymfonyBundleUtil(Project project) {
        this.project = project;
    }

    public Collection<SymfonyBundle> getBundles() {
        if(symfonyBundles != null) {
            return symfonyBundles;
        }

        symfonyBundles = new ArrayList<>();

        for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, "\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle")) {
            symfonyBundles.add(new SymfonyBundle(phpClass));
        }

        return symfonyBundles;
    }

    public Map<String, SymfonyBundle> getParentBundles() {
        Map<String, SymfonyBundle> bundles = new HashMap<>();

        for (SymfonyBundle bundle : getBundles()) {
            if(bundle.getParentBundleName() != null) {
                bundles.put(bundle.getName(), bundle);
            }
        }

        return bundles;
    }

    public Collection<SymfonyBundle> getBundle(String bundleName) {
        return getBundles()
            .stream()
            .filter(
                symfonyBundle -> bundleName.equals(symfonyBundle.getName())
            )
            .collect(Collectors.toSet());
    }

    @Nullable
    public SymfonyBundle getContainingBundle(PhpClass phpClass) {
        for(SymfonyBundle bundle : getBundles()) {
            if(bundle.isInBundle(phpClass)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(PsiFile psiFile) {
        for(SymfonyBundle bundle : getBundles()) {
            if(bundle.isInBundle(psiFile)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(VirtualFile virtualFile) {
        for(SymfonyBundle bundle : getBundles()) {
            if(bundle.isInBundle(virtualFile)) {
                return bundle;
            }
        }

        return null;
    }

    @Nullable
    public SymfonyBundle getContainingBundle(PsiDirectory directory) {
        for(SymfonyBundle bundle : getBundles()) {
            if(bundle.isInBundle(directory.getVirtualFile())) {
                return bundle;
            }
        }

        return null;
    }
}
