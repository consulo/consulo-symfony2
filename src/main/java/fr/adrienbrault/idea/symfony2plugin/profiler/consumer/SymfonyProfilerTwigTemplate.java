package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerTwigTemplate {
    private final String myName;
    private final @Nullable String myPath;
    private final int myRenderCount;

    public SymfonyProfilerTwigTemplate(String name, @Nullable String path, int renderCount) {
        myName = name;
        myPath = path;
        myRenderCount = renderCount;
    }

    public String getName() {
        return myName;
    }

    public @Nullable String getPath() {
        return myPath;
    }

    public int getRenderCount() {
        return myRenderCount;
    }

    public String component1() {
        return myName;
    }

    public @Nullable String component2() {
        return myPath;
    }

    public int component3() {
        return myRenderCount;
    }

    public SymfonyProfilerTwigTemplate copy(String name, @Nullable String path, int renderCount) {
        return new SymfonyProfilerTwigTemplate(name, path, renderCount);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTwigTemplate that)) {
            return false;
        }
        return myName.equals(that.myName)
            && Objects.equals(myPath, that.myPath)
            && myRenderCount == that.myRenderCount;
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + (myPath == null ? 0 : myPath.hashCode());
        result = 31 * result + Integer.hashCode(myRenderCount);
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTwigTemplate(name=" + myName + ", path=" + myPath + ", renderCount=" + myRenderCount + ")";
    }
}
