package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerRequestInterface {
    String getHash();

    /** Unix timestamp in seconds, when exposed by the profiler index. */
    @Nullable
    default Long getTime() {
        return null;
    }

    @Nullable
    String getMethod();

    String getUrl();

    String getProfilerUrl();

    int getStatusCode();

    <T> @Nullable T getCollector(Class<T> classFactory);
}
