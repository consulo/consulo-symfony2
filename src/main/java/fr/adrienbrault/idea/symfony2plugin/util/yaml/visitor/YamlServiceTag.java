package fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor;

import fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml.AttributeResolverInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml.YamlMappingAttributeResolver;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Abstract visitor, so we can support as YAMLHash in future
 * Don't to directly use YAMLHash here
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceTag implements ServiceTagInterface {
    private final String serviceId;

    private final String tagName;

    private final AttributeResolverInterface attributeResolver;

    public YamlServiceTag(String serviceId, String tagName, AttributeResolverInterface attributeResolver) {
        this.serviceId = serviceId;
        this.tagName = tagName;
        this.attributeResolver = attributeResolver;
    }

    public YamlServiceTag(String serviceId, String tagName, YAMLMapping yamlMapping) {
        this(serviceId, tagName, new YamlMappingAttributeResolver(yamlMapping));
    }

    @Override
    public String getServiceId() {
        return this.serviceId;
    }

    public String getName() {
        return tagName;
    }

    /**
     * Our abstract method to get tag attributes
     *
     * @param attr yaml hash attribute to get value of
     * @return value
     */
    public @Nullable String getAttribute(String attr) {
        return attributeResolver.getAttribute(attr);
    }
}
