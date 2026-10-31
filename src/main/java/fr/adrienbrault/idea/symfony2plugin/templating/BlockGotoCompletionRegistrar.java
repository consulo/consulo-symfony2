package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.annotation.component.ExtensionImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * {{ block('foo_block') }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class BlockGotoCompletionRegistrar implements GotoCompletionRegistrar {
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {{ block('foo_block') }}
        registrar.register(TwigPattern.getPrintBlockOrTagFunctionPattern("block"), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new BlockFunctionReferenceCompletionProvider(psiElement);
        });
    }

    private static class BlockFunctionReferenceCompletionProvider extends GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
        private BlockFunctionReferenceCompletionProvider(PsiElement element) {
            super(element);
        }

        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String blockName = PsiElementUtils.trimQuote(element.getText());
            if(StringUtils.isBlank(blockName)) {
                return Collections.emptyList();
            }

            Collection<PsiFile> explicitTemplateTargets = TwigBlockUtil.getBlockFunctionTemplateTargets(element);
            if (!explicitTemplateTargets.isEmpty()) {
                return explicitTemplateTargets.stream()
                    .map(psiFile -> TwigBlockUtil.getBlockOverwriteTargets(psiFile, blockName, true))
                    .flatMap(Collection::stream)
                    .filter(psiElement -> psiElement != element)
                    .collect(Collectors.toSet());
            }

            Collection<PsiElement> psiElements = new HashSet<>();

            psiElements.addAll(
                TwigBlockUtil.getBlockOverwriteTargets(element.getContainingFile(), blockName, true)
            );

            psiElements.addAll(
                TwigBlockUtil.getBlockImplementationTargets(element)
            );

            // filter self navigation
            return psiElements.stream()
                .filter(psiElement -> psiElement != element)
                .collect(Collectors.toSet());
        }

        @Override
        public void getLookupElements(GotoCompletionProviderLookupArguments arguments) {
            Collection<PsiFile> explicitTemplateTargets = TwigBlockUtil.getBlockFunctionTemplateTargets(getElement());
            Collection<PsiFile> templateTargets = explicitTemplateTargets.isEmpty()
                ? Collections.singleton(arguments.getParameters().getOriginalFile())
                : explicitTemplateTargets;

            Collection<LookupElement> lookupElements = TwigUtil.getBlockLookupElements(
                getProject(),
                TwigBlockUtil.getBlockLookupScopeFiles(templateTargets)
            );
            CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(arguments.getParameters(), arguments.getResultSet());
            completionResultSet.addAllElements(lookupElements);
        }
    }
}
