package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerTwig {
    private final double myRenderTimeMs;
    private final int myTemplateCallCount;
    private final int myBlockCallCount;
    private final int myMacroCallCount;
    private final List<SymfonyProfilerTwigTemplate> myRenderedTemplates;
    private final SymfonyProfilerTwigProfile myRoot;

    public SymfonyProfilerTwig(double renderTimeMs, int templateCallCount, int blockCallCount, int macroCallCount, List<SymfonyProfilerTwigTemplate> renderedTemplates, SymfonyProfilerTwigProfile root) {
        myRenderTimeMs = renderTimeMs;
        myTemplateCallCount = templateCallCount;
        myBlockCallCount = blockCallCount;
        myMacroCallCount = macroCallCount;
        myRenderedTemplates = renderedTemplates;
        myRoot = root;
    }

    public double getRenderTimeMs() {
        return myRenderTimeMs;
    }

    public int getTemplateCallCount() {
        return myTemplateCallCount;
    }

    public int getBlockCallCount() {
        return myBlockCallCount;
    }

    public int getMacroCallCount() {
        return myMacroCallCount;
    }

    public List<SymfonyProfilerTwigTemplate> getRenderedTemplates() {
        return myRenderedTemplates;
    }

    public SymfonyProfilerTwigProfile getRoot() {
        return myRoot;
    }

    public double component1() {
        return myRenderTimeMs;
    }

    public int component2() {
        return myTemplateCallCount;
    }

    public int component3() {
        return myBlockCallCount;
    }

    public int component4() {
        return myMacroCallCount;
    }

    public List<SymfonyProfilerTwigTemplate> component5() {
        return myRenderedTemplates;
    }

    public SymfonyProfilerTwigProfile component6() {
        return myRoot;
    }

    public SymfonyProfilerTwig copy(double renderTimeMs, int templateCallCount, int blockCallCount, int macroCallCount, List<SymfonyProfilerTwigTemplate> renderedTemplates, SymfonyProfilerTwigProfile root) {
        return new SymfonyProfilerTwig(renderTimeMs, templateCallCount, blockCallCount, macroCallCount, renderedTemplates, root);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTwig that)) {
            return false;
        }
        return Double.compare(myRenderTimeMs, that.myRenderTimeMs) == 0
            && myTemplateCallCount == that.myTemplateCallCount
            && myBlockCallCount == that.myBlockCallCount
            && myMacroCallCount == that.myMacroCallCount
            && myRenderedTemplates.equals(that.myRenderedTemplates)
            && myRoot.equals(that.myRoot);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(myRenderTimeMs);
        result = 31 * result + Integer.hashCode(myTemplateCallCount);
        result = 31 * result + Integer.hashCode(myBlockCallCount);
        result = 31 * result + Integer.hashCode(myMacroCallCount);
        result = 31 * result + myRenderedTemplates.hashCode();
        result = 31 * result + myRoot.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTwig(renderTimeMs=" + myRenderTimeMs + ", templateCallCount=" + myTemplateCallCount + ", blockCallCount=" + myBlockCallCount + ", macroCallCount=" + myMacroCallCount + ", renderedTemplates=" + myRenderedTemplates + ", root=" + myRoot + ")";
    }
}
