package fr.adrienbrault.idea.symfony2plugin.vite;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSProperty;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.javascript.language.JavaScriptLanguage;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.editor.ui.navigation.TargetPresentationProvider;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.navigation.TargetPresentation;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Line marker for JavaScript/TypeScript files that are Vite entry points.
 *
 * When a JS/TS file is referenced in a vite.config.js/ts rollupOptions.input,
 * a gutter icon appears at the top of the file. The navigation popup shows:
 * - The JSProperty in the Vite config defining that entry
 * - All Twig templates that call vite_entry_link_tags / vite_entry_script_tags with that entry name
 *
 * Uses {@link ViteEntryStubIndex} for reverse lookup (file path → entry name) and
 * {@link ViteTwigUsageStubIndex} for finding Twig usages, both resolved lazily.
 *
 * Registered for JavaScript (covers TypeScript as a dialect).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteEntryStubIndex
 * @see ViteTwigUsageStubIndex
 */
@ExtensionImpl
public class ViteJavaScriptLineMarkerProvider implements LineMarkerProvider {

    @RequiredReadAction
    @Override
    public @Nullable LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        return null;
    }

    @RequiredReadAction
    @Override
    public void collectSlowLineMarkers(List<PsiElement> elements, Collection<LineMarkerInfo> result) {
        if (elements.isEmpty() || !Symfony2ProjectComponent.isEnabled(elements.get(0))) {
            return;
        }

        Collection<VirtualFile> viteConfigFiles = null;

        for (PsiElement element : elements) {
            // Only fire once per file — match on the JSFile node itself
            if (!(element instanceof JSFile jsFile)) {
                continue;
            }

            VirtualFile virtualFile = jsFile.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }

            Collection<VirtualFile> configs = viteConfigFiles;
            if (configs == null) {
                configs = ViteUtil.INSTANCE.getViteConfigFiles(jsFile.getProject());
                if (configs.isEmpty()) {
                    return;
                }
                viteConfigFiles = configs;
            }

            Set<String> entryNames = new LinkedHashSet<>();

            for (VirtualFile configVirtualFile : configs) {
                VirtualFile configDir = configVirtualFile.getParent();
                if (configDir == null) {
                    continue;
                }

                String relPath = VirtualFileUtil.getRelativePath(virtualFile, configDir, '/');
                if (relPath == null) {
                    continue;
                }

                String normalized = relPath;
                if (normalized.startsWith("./")) {
                    normalized = normalized.substring(2);
                }
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }

                entryNames.addAll(FileBasedIndex.getInstance()
                    .getValues(ViteEntryStubIndexKt.VITE_ENTRY_STUB_INDEX_KEY, normalized, GlobalSearchScope.fileScope(jsFile.getProject(), configVirtualFile)));
            }

            if (entryNames.isEmpty()) {
                continue;
            }

            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTargets(LazyValue.<Collection<? extends PsiElement>>notNull(() -> resolveTargets(jsFile, entryNames)))
                .setTooltipText("Vite entry point")
                .setPopupTitle("Vite Entry")
                // consulo-port: PsiTargetPresentationRenderer (setTargetRenderer) -> TargetPresentationProvider (setPresentationProvider)
                .setPresentationProvider(new ViteEntryTargetRenderer());

            result.add(FileLineMarkerUtil.createLineMarkerInfo(builder, jsFile));
        }
    }

    @Override
    public Language getLanguage() {
        return JavaScriptLanguage.INSTANCE;
    }

    private Collection<PsiElement> resolveTargets(JSFile element, Set<String> entryNames) {
        Project project = element.getProject();
        Set<PsiElement> targets = new LinkedHashSet<>();

        // Config JSProperty targets
        for (ViteEntry entry : ViteUtil.INSTANCE.getEntries(project)) {
            if (entryNames.contains(entry.getName()) && entry.getPsiElement() != null) {
                targets.add(entry.getPsiElement());
            }
        }

        // Twig template targets via ViteTwigUsageStubIndex
        PsiManager psiManager = PsiManager.getInstance(project);
        for (String entryName : entryNames) {
            for (VirtualFile twigVirtualFile : FileBasedIndex.getInstance()
                .getContainingFiles(ViteTwigUsageStubIndexKt.VITE_TWIG_USAGE_STUB_INDEX_KEY, entryName, GlobalSearchScope.projectScope(project))) {
                PsiFile psiFile = psiManager.findFile(twigVirtualFile);
                if (psiFile instanceof TwigFile) {
                    targets.add(psiFile);
                }
            }
        }

        return targets;
    }

    private static class ViteEntryTargetRenderer implements TargetPresentationProvider<PsiElement> {
        @RequiredReadAction
        @Override
        public TargetPresentation getPresentation(PsiElement element) {
            String containerText = getContainerText(element);
            return Application.get().getInstance(PsiTargetPresentationFactory.class)
                .presentationBuilder(element)
                .withPresentableText(LocalizeValue.of(getElementText(element)))
                .withContainerText(containerText != null ? LocalizeValue.of(containerText) : LocalizeValue.empty())
                .withIcon(getIcon(element))
                .build();
        }

        public String getElementText(PsiElement element) {
            if (element instanceof JSProperty property) {
                String name = property.getName();
                return name != null ? name : "?";
            }
            else if (element instanceof TwigFile twigFile) {
                return twigFile.getName();
            }

            PsiFile containingFile = element.getContainingFile();
            return containingFile != null ? containingFile.getName() : "?";
        }

        public @Nullable String getContainerText(PsiElement element) {
            PsiFile containingFile = element.getContainingFile();
            VirtualFile file = containingFile != null ? containingFile.getVirtualFile() : null;
            if (file == null) {
                return null;
            }

            String relativePath = VirtualFileUtil.getRelativePath(file, ProjectUtil.getProjectDir(element), '/');
            return relativePath != null ? relativePath : file.getName();
        }

        @RequiredReadAction
        public @Nullable Image getIcon(PsiElement element) {
            return IconDescriptorUpdaters.getIcon(element, 0);
        }
    }
}
