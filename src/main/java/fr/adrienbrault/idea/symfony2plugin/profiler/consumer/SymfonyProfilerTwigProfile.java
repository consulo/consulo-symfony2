package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SymfonyProfilerTwigProfile {
    private final String myTemplate;
    private final String myType;
    private final String myName;
    private final double myDurationMs;
    private final List<SymfonyProfilerTwigProfile> myChildren;

    public SymfonyProfilerTwigProfile(String template, String type, String name, double durationMs, List<SymfonyProfilerTwigProfile> children) {
        myTemplate = template;
        myType = type;
        myName = name;
        myDurationMs = durationMs;
        myChildren = children;
    }

    public String getTemplate() {
        return myTemplate;
    }

    public String getType() {
        return myType;
    }

    public String getName() {
        return myName;
    }

    public double getDurationMs() {
        return myDurationMs;
    }

    public List<SymfonyProfilerTwigProfile> getChildren() {
        return myChildren;
    }

    public String component1() {
        return myTemplate;
    }

    public String component2() {
        return myType;
    }

    public String component3() {
        return myName;
    }

    public double component4() {
        return myDurationMs;
    }

    public List<SymfonyProfilerTwigProfile> component5() {
        return myChildren;
    }

    public SymfonyProfilerTwigProfile copy(String template, String type, String name, double durationMs, List<SymfonyProfilerTwigProfile> children) {
        return new SymfonyProfilerTwigProfile(template, type, name, durationMs, children);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerTwigProfile that)) {
            return false;
        }
        return myTemplate.equals(that.myTemplate)
            && myType.equals(that.myType)
            && myName.equals(that.myName)
            && Double.compare(myDurationMs, that.myDurationMs) == 0
            && myChildren.equals(that.myChildren);
    }

    @Override
    public int hashCode() {
        int result = myTemplate.hashCode();
        result = 31 * result + myType.hashCode();
        result = 31 * result + myName.hashCode();
        result = 31 * result + Double.hashCode(myDurationMs);
        result = 31 * result + myChildren.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerTwigProfile(template=" + myTemplate + ", type=" + myType + ", name=" + myName + ", durationMs=" + myDurationMs + ", children=" + myChildren + ")";
    }
}
