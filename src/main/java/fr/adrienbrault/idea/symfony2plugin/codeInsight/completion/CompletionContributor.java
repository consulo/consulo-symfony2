package fr.adrienbrault.idea.symfony2plugin.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterfaceEx;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class CompletionContributor extends consulo.language.editor.completion.CompletionContributor {

    public CompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                GotoCompletionProviderLookupArguments arguments = null;
                Collection<GotoCompletionContributor> contributors = GotoCompletionUtil.getContributors(psiElement);
                for (GotoCompletionContributor contributor : contributors) {
                    GotoCompletionProviderInterface formReferenceCompletionContributor = contributor.getProvider(psiElement);
                    if (formReferenceCompletionContributor != null) {
                        completionResultSet.addAllElements(
                            formReferenceCompletionContributor.getLookupElements()
                        );
                    }

                    // extension to provide full argument pipes
                    if (formReferenceCompletionContributor instanceof GotoCompletionProviderInterfaceEx) {
                        if (arguments == null) {
                            arguments = new GotoCompletionProviderLookupArguments(completionParameters, processingContext, completionResultSet);
                        }

                        ((GotoCompletionProviderInterfaceEx) formReferenceCompletionContributor).getLookupElements(arguments);
                    }
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
