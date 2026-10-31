package fr.adrienbrault.idea.symfony2plugin.util.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, final CompletionResultSet completionResultSet) {
        Project project = completionParameters.getPosition().getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        completionResultSet.addAllElements(
            EventDispatcherSubscriberUtil.getEventNameLookupElements(project)
        );
    }
}