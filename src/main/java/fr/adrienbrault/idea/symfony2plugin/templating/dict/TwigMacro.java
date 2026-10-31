package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMacro {
    private final String name;

    private final String template;

    @Nullable
    private String originalName;

    @Nullable
    private String parameter;

    public TwigMacro(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public TwigMacro(String name, String template, String originalName) {
        this(name, template);
        this.originalName = originalName;
    }

    @Nullable
    public String getOriginalName() {
        return originalName;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

    @Nullable
    public String getParameter() {
        return parameter;
    }

    public TwigMacro withParameter(@Nullable String parameter) {
        this.parameter = parameter;
        return this;
    }
}
