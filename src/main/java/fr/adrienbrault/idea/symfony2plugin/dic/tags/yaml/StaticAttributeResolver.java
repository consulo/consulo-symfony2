package fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StaticAttributeResolver implements AttributeResolverInterface {
    private final Map<String, String> map;

    public StaticAttributeResolver(String key, String value) {
        this.map = new HashMap<>();
        this.map.put(key, value);
    }
    public StaticAttributeResolver(Map<String, String> map) {
        this.map = map;
    }

    @Nullable
    @Override
    public String getAttribute(String attr) {
        if(this.map.containsKey(attr)) {
            return this.map.get(attr);
        }

        return null;
    }
}
