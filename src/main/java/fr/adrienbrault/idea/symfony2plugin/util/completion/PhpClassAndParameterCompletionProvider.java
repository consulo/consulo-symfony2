package fr.adrienbrault.idea.symfony2plugin.util.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.ParameterPercentWrapInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassAndParameterCompletionProvider implements CompletionProvider {

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, final CompletionResultSet resultSet) {
        PsiElement psiElement = parameters.getOriginalPosition();
        if (psiElement == null) {
            return;
        }

        Project project = parameters.getPosition().getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PhpClassCompletionProvider.addClassCompletion(parameters, resultSet, psiElement, false);

        for( Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(project).entrySet()) {
            resultSet.addElement(
                new ParameterLookupElement(entry.getValue(), ParameterPercentWrapInsertHandler.getInstance(), psiElement.getText())
            );
        }

    }

}
