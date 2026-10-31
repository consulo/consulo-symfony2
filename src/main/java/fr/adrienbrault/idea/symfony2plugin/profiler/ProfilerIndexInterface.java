package fr.adrienbrault.idea.symfony2plugin.profiler;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerIndexInterface {
    List<ProfilerRequestInterface> getRequests();

    @Nullable
    String getUrlForRequest(ProfilerRequestInterface request);

    /**
     * Returns the stored serialized profile for collector-level inspection, when supported.
     * Remote profiler indexes can keep the default until they expose an equivalent raw source.
     */
    default byte @Nullable [] getRawProfile(String hash) {
        return null;
    }
}
