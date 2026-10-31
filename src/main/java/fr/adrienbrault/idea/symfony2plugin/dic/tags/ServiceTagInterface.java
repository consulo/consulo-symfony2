package fr.adrienbrault.idea.symfony2plugin.dic.tags;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceTagInterface {

    String getServiceId();

    String getName();

    /**
     * Our abstract method to get tag attributes
     *
     * @param attr yaml hash attribute to get value of
     * @return value
     */
    @Nullable
    String getAttribute(String attr);
}
