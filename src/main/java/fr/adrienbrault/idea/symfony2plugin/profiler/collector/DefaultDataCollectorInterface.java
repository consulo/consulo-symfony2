package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DefaultDataCollectorInterface {
    /**
     * Resolved request controller, eg "App\\Controller\\BlogController::show".
     */
    @Nullable
    String getController();

    /**
     * Matched route name, eg "blog_show".
     */
    @Nullable
    String getRoute();

    /**
     * Symfony profiler entry view, eg "blog/show.html.twig".
     */
    @Nullable
    String getTemplate();

    /**
     * Rendered Twig templates in profiler order, eg "blog/show.html.twig", "base.html.twig".
     */
    default Collection<String> getRenderedTemplates() {
        return Collections.emptyList();
    }

    /**
     * Root form types used by the request, eg "App\\Form\\SearchType".
     */
    Collection<String> getFormTypes();
}
