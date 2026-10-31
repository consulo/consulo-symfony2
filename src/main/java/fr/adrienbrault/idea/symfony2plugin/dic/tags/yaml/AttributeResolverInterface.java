package fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AttributeResolverInterface {
    @Nullable
    String getAttribute(String attr);
}
