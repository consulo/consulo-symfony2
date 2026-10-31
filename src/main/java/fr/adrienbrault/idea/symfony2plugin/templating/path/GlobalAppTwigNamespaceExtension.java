package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * app/Resources/views/foo.html.twig => :foo.html.twig
 * app/Resources/views/foo.html.twig => foo.html.twig
 *
 * /templates/foo.html.twig => foo.html.twig
 * /templates/foo.html.twig => :foo.html.twig
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after ContainerTwigNamespaceExtension")
public class GlobalAppTwigNamespaceExtension implements TwigNamespaceExtension {
    @Override
    public Collection<TwigPath> getNamespaces(TwigNamespaceExtensionParameter parameter) {
        VirtualFile baseDir = ProjectUtil.getProjectDir(parameter.getProject());

        // "app" folder
        Collection<VirtualFile> directories = FilesystemUtil.getAppDirectories(parameter.getProject()).stream()
            .map(path -> VirtualFileUtil.findRelativeFile(path, "Resources", "views"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // flex "templates" in root
        VirtualFile templates = VirtualFileUtil.findRelativeFile(baseDir, "templates");
        if (templates != null) {
            directories.add(templates);
        }

        Collection<TwigPath> paths = new ArrayList<>();

        for (VirtualFile dir : directories) {
            String relativePath = VfsExUtil.getRelativeProjectPathStrict(parameter.getProject(), dir);
            if (relativePath == null) {
                continue;
            }

            if (TwigUtil.hasBundleNamespaceSupport(parameter.getProject())) {
                paths.add(new TwigPath(relativePath, TwigUtil.MAIN, TwigUtil.NamespaceType.BUNDLE));
            }

            paths.add(new TwigPath(relativePath, TwigUtil.MAIN, TwigUtil.NamespaceType.ADD_PATH));
        }

        return paths;
    }
}
