package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceNameStrategyInterface {
    @Nullable String getServiceName(ServiceNameStrategyParameter parameter);
}
