package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.util.ProcessingContext;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GotoCompletionProviderLookupArguments {
    private final CompletionParameters parameters;

    private final ProcessingContext context;

    private final CompletionResultSet resultSet;

    public GotoCompletionProviderLookupArguments(CompletionParameters parameters, ProcessingContext context, CompletionResultSet resultSet) {
        this.parameters = parameters;
        this.context = context;
        this.resultSet = resultSet;
    }

    public CompletionParameters getParameters() {
        return parameters;
    }

    public ProcessingContext getContext() {
        return context;
    }

    public CompletionResultSet getResultSet() {
        return resultSet;
    }

    public void addAllElements(Iterable<? extends LookupElement> elements) {
        resultSet.addAllElements(elements);
    }
}
