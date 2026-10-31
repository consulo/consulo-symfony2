package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtension {
    @Nullable
    final private String signature;

    final private TwigExtensionParser.TwigExtensionType twigExtensionType;

    final private Map<String, String> options = new HashMap<>();

    private Collection<String> types = Collections.emptyList();

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType) {
        this(twigExtensionType, null);
    }

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature) {
        this.twigExtensionType = twigExtensionType;
        this.signature = signature;
    }

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature, Map<String, String> options) {
        this(twigExtensionType, signature);
        this.options.putAll(options);
    }

    public TwigExtension(TwigExtensionParser.TwigExtensionType twigExtensionType, @Nullable String signature, Map<String, String> options, Collection<String> types) {
        this(twigExtensionType, signature);
        this.options.putAll(options);
        this.types = new HashSet<>(types);
    }

    public TwigExtensionParser.TwigExtensionType getTwigExtensionType() {
        return twigExtensionType;
    }

    public String getType() {
        return twigExtensionType.toString();
    }

    @Nullable
    public String getSignature() {
        return signature;
    }

    @Nullable
    public String getOption(String key) {
        return options.getOrDefault(key, null);
    }

    public Collection<String> getTypes() {
        return types;
    }

    public boolean isDeprecated() {
        return options.containsKey("deprecated") || options.containsKey("deprecation_info");
    }
}
