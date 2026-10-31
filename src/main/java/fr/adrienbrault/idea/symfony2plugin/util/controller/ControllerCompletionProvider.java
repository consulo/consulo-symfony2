package fr.adrienbrault.idea.symfony2plugin.util.controller;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet resultSet) {
        Project project = completionParameters.getPosition().getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        List<LookupElement> lookupElements = ControllerIndex.getControllerLookupElements(project);
        CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(completionParameters, resultSet);
        completionResultSet.addAllElements(lookupElements);

    }
}
