package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpProfilerRequest implements ProfilerRequestInterface {
    private final int statusCode;

    private final String hash;

    private final String profilerUrl;

    @Nullable
    private final String method;

    private final String url;

    private final Object[] collectors;

    public HttpProfilerRequest(ProfilerRequestInterface request, Object... collectors) {
        this(request.getStatusCode(), request.getHash(), request.getProfilerUrl(), request.getMethod(), request.getUrl(), collectors);
    }

    public HttpProfilerRequest(int statusCode, String hash, String profilerUrl, @Nullable String method, String url, @Nullable Object... collectors) {
        this.statusCode = statusCode;
        this.hash = hash;
        this.profilerUrl = profilerUrl;
        this.method = method;
        this.url = url;
        this.collectors = collectors != null ? collectors : new Object[] {};
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Nullable
    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getProfilerUrl() {
        return this.profilerUrl;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getCollector(Class<T> classFactory) {
        for (Object collector : collectors) {
            if(classFactory.isAssignableFrom(collector.getClass())) {
                return (T) collector;
            }
        }

        return null;
    }
}
