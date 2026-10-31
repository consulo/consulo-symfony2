package fr.adrienbrault.idea.symfony2plugin.vite;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides completion and navigation for Vite entry points in Twig templates:
 * - vite_entry_link_tags()
 * - vite_entry_script_tags()
 * - reprise_entry_*()
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ViteGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            RepriseTwigCallUtilKt.viteEntryPattern(),
            element -> {
                if (!Symfony2ProjectComponent.isEnabled(element)) {
                    return null;
                }
                return new ViteEntryProvider(element);
            }
        );
    }

    private static class ViteEntryProvider extends GotoCompletionProvider {

        ViteEntryProvider(PsiElement element) {
            super(element);
        }

        @Override
        public void getLookupElements(GotoCompletionProviderLookupArguments arguments) {
            Set<String> seen = new HashSet<>();
            List<LookupElement> lookupElements = new ArrayList<>();
            for (ViteEntry entry : ViteUtil.INSTANCE.getEntries(getProject())) {
                if (!seen.add(entry.getName())) {
                    continue;
                }

                lookupElements.add(
                    LookupElementBuilder.create(entry.getName())
                        .withIcon(Symfony2Icons.SYMFONY)
                        .withTypeText(entry.getTargetPath() != null ? entry.getTargetPath() : entry.getConfigFile().getName())
                );
            }

            CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(arguments.getParameters(), arguments.getResultSet());
            completionResultSet.addAllElements(lookupElements);
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = element.getText();
            if (StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Set<PsiElement> targets = new LinkedHashSet<>();

            for (ViteEntry entry : ViteUtil.INSTANCE.getEntries(getProject())) {
                if (entry.getName().equals(contents)) {
                    String path = entry.getTargetPath();
                    if (path != null) {
                        String normalized = path.replace("\\", "/");
                        if (normalized.startsWith("./")) {
                            normalized = normalized.substring(2);
                        }

                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(entry.getConfigFile().getParent(), normalized.split("/", -1));
                        if (relativeFile != null) {
                            PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(relativeFile);
                            if (psiFile != null) {
                                targets.add(psiFile);
                            }
                        }
                    }

                    PsiElement psiElement = entry.getPsiElement();
                    if (psiElement != null) {
                        targets.add(psiElement);
                    }
                    else {
                        PsiFile configFile = PsiManager.getInstance(getProject()).findFile(entry.getConfigFile());
                        if (configFile != null) {
                            targets.add(configFile);
                        }
                    }
                }
            }

            return targets;
        }
    }
}
