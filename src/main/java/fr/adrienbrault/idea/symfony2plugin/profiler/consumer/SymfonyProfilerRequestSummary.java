package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerRequestSummary {
    private final @Nullable String myMethod;
    private final @Nullable String myPath;
    private final @Nullable String myRoute;
    private final @Nullable Integer myStatusCode;
    private final @Nullable String myContentType;

    public SymfonyProfilerRequestSummary(@Nullable String method, @Nullable String path, @Nullable String route, @Nullable Integer statusCode, @Nullable String contentType) {
        myMethod = method;
        myPath = path;
        myRoute = route;
        myStatusCode = statusCode;
        myContentType = contentType;
    }

    public @Nullable String getMethod() {
        return myMethod;
    }

    public @Nullable String getPath() {
        return myPath;
    }

    public @Nullable String getRoute() {
        return myRoute;
    }

    public @Nullable Integer getStatusCode() {
        return myStatusCode;
    }

    public @Nullable String getContentType() {
        return myContentType;
    }

    public @Nullable String component1() {
        return myMethod;
    }

    public @Nullable String component2() {
        return myPath;
    }

    public @Nullable String component3() {
        return myRoute;
    }

    public @Nullable Integer component4() {
        return myStatusCode;
    }

    public @Nullable String component5() {
        return myContentType;
    }

    public SymfonyProfilerRequestSummary copy(@Nullable String method, @Nullable String path, @Nullable String route, @Nullable Integer statusCode, @Nullable String contentType) {
        return new SymfonyProfilerRequestSummary(method, path, route, statusCode, contentType);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerRequestSummary that)) {
            return false;
        }
        return Objects.equals(myMethod, that.myMethod)
            && Objects.equals(myPath, that.myPath)
            && Objects.equals(myRoute, that.myRoute)
            && Objects.equals(myStatusCode, that.myStatusCode)
            && Objects.equals(myContentType, that.myContentType);
    }

    @Override
    public int hashCode() {
        int result = (myMethod == null ? 0 : myMethod.hashCode());
        result = 31 * result + (myPath == null ? 0 : myPath.hashCode());
        result = 31 * result + (myRoute == null ? 0 : myRoute.hashCode());
        result = 31 * result + (myStatusCode == null ? 0 : myStatusCode.hashCode());
        result = 31 * result + (myContentType == null ? 0 : myContentType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerRequestSummary(method=" + myMethod + ", path=" + myPath + ", route=" + myRoute + ", statusCode=" + myStatusCode + ", contentType=" + myContentType + ")";
    }
}
