package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JsonRoute implements RouteInterface {

    private final String name;

    @Nullable
    private String controller;

    @Nullable
    private String path;

    @Nullable
    private Collection<String> methods;

    public JsonRoute(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getController() {
        return this.controller;
    }

    @Nullable
    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public Collection<String> getMethods() {
        return this.methods == null ? Collections.emptyList() : this.methods;
    }

    public JsonRoute setPath(@Nullable String path) {
        this.path = path;

        return this;
    }

    public JsonRoute setController(@Nullable String controller) {
        this.controller = controller;

        return this;
    }

    public JsonRoute setMethods(@Nullable Collection<String> methods) {
        this.methods = methods;

        return this;
    }
}
