package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerRequest implements ProfilerRequestInterface {

    private final List<String> separatedLine = new ArrayList<>();

    private Object[] collectors = new Object[] {};

    public LocalProfilerRequest(String[] separatedLine) {
        Collections.addAll(this.separatedLine, separatedLine);
    }

    public LocalProfilerRequest(String[] separatedLine, Object... collectors) {
        this(separatedLine);
        this.collectors = collectors;
    }

    public String getHash() {
        return this.separatedLine.get(0);
    }

    @Nullable
    @Override
    public Long getTime() {
        if (this.separatedLine.size() <= 4) {
            return null;
        }

        try {
            return Long.parseLong(this.separatedLine.get(4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    public String getMethod() {
        return this.separatedLine.get(2);
    }

    public String getUrl() {
        return this.separatedLine.get(3);
    }

    @Override
    public String getProfilerUrl() {
        return "_profiler/" + this.getHash();
    }

    @Override
    public int getStatusCode() {
        if(this.separatedLine.size() <= 6) {
            return 0;
        }

        try {
            return Integer.parseInt(this.separatedLine.get(6));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

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
