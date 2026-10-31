package fr.adrienbrault.idea.symfony2plugin.twig.variable.globals;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigGlobalVariable {
    private final String name;

    private final String value;

    private final TwigGlobalEnum twigGlobalEnum;


    TwigGlobalVariable(String name, String value, TwigGlobalEnum twigGlobalEnum) {
        this.name = name;
        this.value = value;
        this.twigGlobalEnum = twigGlobalEnum;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public TwigGlobalEnum getTwigGlobalEnum() {
        return twigGlobalEnum;
    }
}
