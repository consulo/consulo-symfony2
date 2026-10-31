package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils;

import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceSuggestionUtil {

    private static Collection<String> createServiceCollection(ServiceTypeHint serviceTypeHint, Collection<ContainerService> serviceMap) {
        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionsForTypeHint(
            serviceTypeHint.getMethod(),
            serviceTypeHint.getIndex(),
            serviceMap
        );

        if(suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(suggestions, ContainerService::getName);
    }

    public static Collection<String> createSuggestions(@Nullable ServiceTypeHint serviceTypeHint, Collection<ContainerService> serviceMap) {
        if(serviceTypeHint == null) {
            return Collections.emptyList();
        }

        return ServiceSuggestionUtil.createServiceCollection(serviceTypeHint, serviceMap);
    }
}
