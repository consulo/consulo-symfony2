package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerParameter {
    final private String name;
    @Nullable
    final private String value;
    final private boolean isWeak;

    @Deprecated
    public ContainerParameter(String name, @Nullable String value) {
        this(name, value, false);
    }

    public ContainerParameter(String name, @Nullable String value, boolean isWeak) {
        this.name = name;
        this.value = value;
        this.isWeak = isWeak;
    }

    public ContainerParameter(String name, boolean isWeak) {
        this.name = name;
        this.isWeak = isWeak;
        this.value = null;
    }

    public ContainerParameter(String name, Collection<String> values, boolean isWeak) {
        this.name = name;
        this.value = values.isEmpty() ? null : values.iterator().next();
        this.isWeak = isWeak;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public boolean isWeak() {
        return isWeak;
    }
}

