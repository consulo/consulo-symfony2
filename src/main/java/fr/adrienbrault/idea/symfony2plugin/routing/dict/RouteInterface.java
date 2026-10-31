package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface RouteInterface {

    String getName();

    @Nullable
    String getController();

    @Nullable
    String getPath();

    Collection<String> getMethods();
}
