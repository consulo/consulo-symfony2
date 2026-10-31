package fr.adrienbrault.idea.symfony2plugin.templating.webpack;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterfaceEx;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * - encore_entry_link_tags()
 * - encore_entry_script_tags()
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class WebpackEncoreGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(TwigPattern.getPrintBlockOrTagFunctionPattern("encore_entry_link_tags", "encore_entry_script_tags"), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new WebpackEncoreGotoCompletionRegistrar.EncoreEntry(psiElement);
        });
    }

    private static class EncoreEntry extends GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
        private EncoreEntry(PsiElement element) {
            super(element);
        }

        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = element.getText();

            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            HashSet<PsiElement> targets = new HashSet<>();
            SymfonyWebpackUtil.visitAllEntryFileTypes(element.getProject(), triple -> {
                if (contents.equalsIgnoreCase(triple.getEntry())) {
                    String entryTarget = triple.getEntryTarget();
                    if (entryTarget != null) {
                        // "./foobar" => "foobar"
                        String replace = entryTarget.replace("\\", "/");
                        if (replace.startsWith("./")) {
                            replace = replace.substring(2);
                        }

                        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(triple.getVirtualFile().getParent(), replace.split("/"));
                        if (relativeFile != null) {
                            PsiFile file = PsiManager.getInstance(getProject()).findFile(relativeFile);
                            if (file != null) {
                                targets.add(file);
                            }
                        }
                    }

                    PsiElement target = triple.getPsiElement();
                    if (target != null) {
                        targets.add(target);
                    } else {
                        PsiFile file = PsiManager.getInstance(getProject()).findFile(triple.getVirtualFile());
                        if (file != null) {
                            targets.add(file);
                        }
                    }
                }
            });

            return targets;
        }

        @Override
        public void getLookupElements(GotoCompletionProviderLookupArguments arguments) {
            Collection<LookupElement> lookupElements = new ArrayList<>();
            SymfonyWebpackUtil.visitAllEntryFileTypes(getProject(), pair ->
                {
                    LookupElementBuilder lookupElement = LookupElementBuilder.create(pair.getEntry())
                        .withIcon(Symfony2Icons.SYMFONY)
                        .withTypeText(pair.getVirtualFile().getName());

                    lookupElements.add(lookupElement);
                }
            );

            CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(arguments.getParameters(), arguments.getResultSet());
            completionResultSet.addAllElements(lookupElements);
        }
    }
}
