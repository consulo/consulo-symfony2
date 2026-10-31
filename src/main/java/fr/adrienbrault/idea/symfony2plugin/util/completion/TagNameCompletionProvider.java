package fr.adrienbrault.idea.symfony2plugin.util.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.ContainerTagLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TagNameCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        completionResultSet.addAllElements(getTagLookupElements(completionParameters.getPosition().getProject()));
    }

    public static Collection<LookupElement> getTagLookupElements(Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<>();

        Set<String> uniqueTags = new HashSet<>();

        XmlTagParser xmlEventParser = ServiceXmlParserFactory.getInstance(project, XmlTagParser.class);
        for(String tag: xmlEventParser.get()) {
            uniqueTags.add(tag);
            lookupElements.add(new ContainerTagLookupElement(tag));
        }

        for(String serviceName: SymfonyProcessors.createResult(project, ServicesTagStubIndex.KEY)) {
            List<Set<String>> tags = FileBasedIndex.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, ServiceIndexUtil.getRestrictedFileTypesScope(project));
            for(Set<String> tagDef: tags) {
                for(String tag: tagDef) {
                    if(!uniqueTags.contains(tag)) {
                        uniqueTags.add(tag);
                        lookupElements.add(new ContainerTagLookupElement(tag, true));
                    }
                }
            }
        }

        // findTaggedServiceIds("foo") for ContainerBuilder
        for (ContainerBuilderCall call : FileBasedIndex.getInstance().getValues(ContainerBuilderStubIndex.KEY, "findTaggedServiceIds", GlobalSearchScope.allScope(project))) {
            Collection<String> parameter = call.getParameter();
            if(parameter == null || parameter.isEmpty()) {
                continue;
            }

            for (String s : parameter) {
                if(uniqueTags.contains(s)) {
                    continue;
                }

                lookupElements.add(new ContainerTagLookupElement(s, true));
            }
        }

        return lookupElements;
    }
}
