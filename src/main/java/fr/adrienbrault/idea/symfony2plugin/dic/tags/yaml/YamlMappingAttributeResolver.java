package fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml;

import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlMappingAttributeResolver implements AttributeResolverInterface {
    private final YAMLMapping yamlMapping;

    public YamlMappingAttributeResolver(YAMLMapping yamlMapping) {
        this.yamlMapping = yamlMapping;
    }

    @Nullable
    public String getAttribute(String attr) {
        return YamlHelper.getYamlKeyValueAsString(yamlMapping, attr);
    }
}
