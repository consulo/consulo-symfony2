package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlKeyValueAttributeValue extends AttributeValueAbstract {

    private final YAMLKeyValue yamlKeyValue;

    public YamlKeyValueAttributeValue(YAMLKeyValue yamlKeyValue) {
        super(yamlKeyValue);
        this.yamlKeyValue = yamlKeyValue;
    }

    @Nullable
    @Override
    public String getString(String key) {
        String value = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, key);
        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }

    @Override
    public Collection<String> getTags() {
        return YamlHelper.collectServiceTags(yamlKeyValue);
    }

    @Override
    public Collection<String> getStringArray(String key) {
        return YamlHelper.getYamlKeyValueStringOrArray(yamlKeyValue, key);
    }
}
