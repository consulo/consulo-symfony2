package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * "app/Resources/ParentBundle/Resources/views"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after BundleTwigNamespaceExtension")
public class BundleOverwriteNamespaceExtensions implements TwigNamespaceExtension {
    @Override
    public Collection<TwigPath> getNamespaces(TwigNamespaceExtensionParameter parameter) {
        if (!TwigUtil.hasBundleNamespaceSupport(parameter.getProject())) {
            return Collections.emptyList();
        }

        Collection<TwigPath> twigPaths = new ArrayList<>();

        new SymfonyBundleUtil(parameter.getProject()).getParentBundles().forEach((key, virtualFile) -> {
            VirtualFile views = virtualFile.getRelative("Resources/views");
            if (views != null) {
                String path = VfsExUtil.getRelativeProjectPathStrict(parameter.getProject(), views);
                if (path != null) {
                    twigPaths.add(new TwigPath(path, key, TwigUtil.NamespaceType.BUNDLE));
                }
            }
        });

        return twigPaths;
    }
}
