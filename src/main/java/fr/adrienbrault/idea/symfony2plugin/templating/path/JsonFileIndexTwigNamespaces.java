package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigConfigJson;
import fr.adrienbrault.idea.symfony2plugin.templating.path.dict.TwigPathJson;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class JsonFileIndexTwigNamespaces implements TwigNamespaceExtension {

    @Override
    public Collection<TwigPath> getNamespaces(final TwigNamespaceExtensionParameter parameter) {
        return getNamespacesInner(parameter.getProject());
    }

    private static Collection<TwigPath> getNamespacesInner(Project project) {
        Collection<TwigPath> twigPaths = new ArrayList<>();

        Collection<VirtualFile> filesByName = FilenameIndex.getVirtualFilesByName(project, "ide-twig.json", GlobalSearchScope.allScope(project));
        for (final PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, filesByName)) {
            Collection<TwigPath> cachedValue = LanguageCachedValueUtil.getCachedValue(psiFile, new MyJsonCachedValueProvider(psiFile));
            if(cachedValue != null) {
                twigPaths.addAll(cachedValue);
            }
        }

        return twigPaths;
    }

    private record MyJsonCachedValueProvider(PsiFile psiFile) implements CachedValueProvider<Collection<TwigPath>> {
        @Override
        public Result<Collection<TwigPath>> compute() {
            Collection<TwigPath> twigPaths = new ArrayList<>();

            String text = psiFile.getText();
            TwigConfigJson configJson = null;
            try {
                configJson = new Gson().fromJson(text, TwigConfigJson.class);
            } catch (JsonSyntaxException | JsonIOException | IllegalStateException ignored) {
            }

            if (configJson == null) {
                return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
            }

            for (TwigPathJson twigPath : configJson.getNamespaces()) {
                String path = twigPath.getPath();
                if (path == null || path.equals(".")) {
                    path = "";
                }

                path = StringUtils.stripStart(path.replace("\\", "/"), "/");
                PsiDirectory parent = psiFile.getParent();
                if (parent == null) {
                    continue;
                }

                // current directory check and subfolder
                VirtualFile twigRoot;
                if (!path.isEmpty()) {
                    twigRoot = VirtualFileUtil.findRelativeFile(parent.getVirtualFile(), path.split("/"));
                } else {
                    twigRoot = psiFile.getParent().getVirtualFile();
                }

                if (twigRoot == null) {
                    continue;
                }

                String relativePath = VfsExUtil.getRelativeProjectPath(psiFile.getProject(), twigRoot);
                if (relativePath == null) {
                    continue;
                }

                String namespace = twigPath.getNamespace();

                TwigUtil.NamespaceType pathType = TwigUtil.NamespaceType.ADD_PATH;
                String type = twigPath.getType();
                if ("bundle".equalsIgnoreCase(type)) {
                    pathType = TwigUtil.NamespaceType.BUNDLE;
                }

                String namespacePath = StringUtils.stripStart(relativePath, "/");

                if (StringUtils.isNotBlank(namespace)) {
                    twigPaths.add(new TwigPath(namespacePath, namespace, pathType, true));
                } else {
                    twigPaths.add(new TwigPath(namespacePath, TwigUtil.MAIN, pathType, true));
                }
            }

            return Result.create(twigPaths, psiFile, psiFile.getVirtualFile());
        }
    }
}
