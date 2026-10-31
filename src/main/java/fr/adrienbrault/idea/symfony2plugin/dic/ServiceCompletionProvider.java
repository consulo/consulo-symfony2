package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionSorter;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.*;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceCompletionProvider implements CompletionProvider {

    private static final ServiceSuggestionCollector[] COLLECTORS = new ServiceSuggestionCollector[] {
        new XmlConstructServiceSuggestionCollector(),
        new YamlConstructServiceSuggestionCollector(),
        new XmlCallServiceSuggestionCollector(),
        new PhpAttributeServiceSuggestionCollector(),
        new PhpArrayArgumentServiceSuggestionCollector(),
        new PhpFluentArgumentServiceSuggestionCollector(),
    };

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet resultSet) {

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        PsiElement element = parameters.getPosition();

        PrioritizedLookupResult result = getLookupElements(
            element,
            ContainerCollectionResolver.getServices(element.getProject()).values()
        );

        addPrioritizedServiceLookupElements(parameters, resultSet, result);
    }

    public static void addPrioritizedServiceLookupElements(CompletionParameters parameters, CompletionResultSet resultSet, PrioritizedLookupResult result) {
        // move known elements to top
        if(!result.getTopStrings().isEmpty()) {
            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(parameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new MyLookupElementWeigher(result.getTopStrings()));

            resultSet = resultSet.withRelevanceSorter(completionSorter);
        }

        resultSet.addAllElements(result.getLookupElements());
    }

    public static PrioritizedLookupResult getLookupElements(@Nullable PsiElement element, Collection<ContainerService> services) {
        // collect instance to highlight services
        List<String> servicesForInstance = new ArrayList<>();

        if(element != null) {
            Set<String> servicesForInstanceSet = new java.util.HashSet<>();

            for (ServiceSuggestionCollector collector : COLLECTORS) {
                servicesForInstanceSet.addAll(collector.collect(element, services));
            }

            servicesForInstance.addAll(
                ServiceContainerUtil.getSortedServiceId(element.getProject(), servicesForInstanceSet)
            );
        }

        Collection<LookupElement> collect = services.stream()
            .map((Function<ContainerService, LookupElement>)
                service -> new ServiceStringLookupElement(service, servicesForInstance.contains(service.getName())))
            .collect(Collectors.toList());

        return new PrioritizedLookupResult(collect, servicesForInstance);
    }

    public static class PrioritizedLookupResult {

        private final Collection<LookupElement> lookupElements;

        private final List<String> topStrings;

        public PrioritizedLookupResult(Collection<LookupElement> lookupElements, List<String> topStrings) {
            this.lookupElements = lookupElements;
            this.topStrings = topStrings;
        }

        public Collection<LookupElement> getLookupElements() {
            return lookupElements;
        }

        public List<String> getTopStrings() {
            return topStrings;
        }
    }

    public static class MyLookupElementWeigher extends LookupElementWeigher {
        private final List<String> elements;

        public MyLookupElementWeigher(List<String> elements) {
            super("topElement");
            this.elements = elements;

            // top most element ist first
            Collections.reverse(elements);
        }

        @Nullable
        @Override
        public Comparable weigh(LookupElement element) {
            String lookupString = element.getLookupString();
            if(!elements.contains(lookupString)) {
                return 0;
            }

            // start by "0"
            int pos = elements.indexOf(lookupString) + 1;

            // we reversed the list so, top most element has higher negative value now
            return -1000 - pos;
        }
    }
}
