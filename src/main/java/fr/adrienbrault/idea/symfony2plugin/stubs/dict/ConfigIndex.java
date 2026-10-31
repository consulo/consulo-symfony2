package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigIndex implements Serializable {
    private final String name;

    private final LinkedHashMap<String, LinkedHashMap<String, String>> configs;

    private final Set<String> values;

    public ConfigIndex(String name, LinkedHashMap<String, LinkedHashMap<String, String>> configs, Set<String> values) {
        this.name = name;
        this.configs = configs;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public Map<String, LinkedHashMap<String, String>> getConfigs() {
        return configs;
    }

    public Set<String> getValues() {
        return values;
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .append(this.configs.hashCode())
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConfigIndex &&
            Objects.equals(((ConfigIndex) obj).name, this.name) &&
            Objects.equals(((ConfigIndex) obj).configs, this.configs);
    }
}
