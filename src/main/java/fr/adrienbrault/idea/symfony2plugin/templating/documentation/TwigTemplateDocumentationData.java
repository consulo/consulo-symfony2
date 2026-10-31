package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Former Kotlin {@code internal data class TwigTemplateDocumentationData} of TwigTemplateDocumentationTargetProvider.kt
 * (Kotlin default arguments: parents = emptyMap(), phpFiles = emptySet(), callers = emptySet(), twigUsages = emptyMap()).
 */
public final class TwigTemplateDocumentationData {
    private final String myName;
    // Virtual-file URL -> presentable path; no retained PSI.
    private final Map<String, String> myPaths;
    private final Map<String, Set<String>> myParents;

    private final Set<String> myPhpFiles;
    private final Set<String> myCallers;

    private final Map<String, Set<String>> myTwigUsages;

    public TwigTemplateDocumentationData(
        String name,
        Map<String, String> paths,
        Map<String, Set<String>> parents,
        Set<String> phpFiles,
        Set<String> callers,
        Map<String, Set<String>> twigUsages
    ) {
        myName = name;
        myPaths = paths;
        myParents = parents;
        myPhpFiles = phpFiles;
        myCallers = callers;
        myTwigUsages = twigUsages;
    }

    public String getName() {
        return myName;
    }

    public Map<String, String> getPaths() {
        return myPaths;
    }

    public Map<String, Set<String>> getParents() {
        return myParents;
    }

    public Set<String> getPhpFiles() {
        return myPhpFiles;
    }

    public Set<String> getCallers() {
        return myCallers;
    }

    public Map<String, Set<String>> getTwigUsages() {
        return myTwigUsages;
    }

    public String component1() {
        return myName;
    }

    public Map<String, String> component2() {
        return myPaths;
    }

    public Map<String, Set<String>> component3() {
        return myParents;
    }

    public Set<String> component4() {
        return myPhpFiles;
    }

    public Set<String> component5() {
        return myCallers;
    }

    public Map<String, Set<String>> component6() {
        return myTwigUsages;
    }

    public TwigTemplateDocumentationData copy(
        String name,
        Map<String, String> paths,
        Map<String, Set<String>> parents,
        Set<String> phpFiles,
        Set<String> callers,
        Map<String, Set<String>> twigUsages
    ) {
        return new TwigTemplateDocumentationData(name, paths, parents, phpFiles, callers, twigUsages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TwigTemplateDocumentationData that)) {
            return false;
        }
        return Objects.equals(myName, that.myName)
            && Objects.equals(myPaths, that.myPaths)
            && Objects.equals(myParents, that.myParents)
            && Objects.equals(myPhpFiles, that.myPhpFiles)
            && Objects.equals(myCallers, that.myCallers)
            && Objects.equals(myTwigUsages, that.myTwigUsages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myPaths, myParents, myPhpFiles, myCallers, myTwigUsages);
    }

    @Override
    public String toString() {
        return "TwigTemplateDocumentationData(name=" + myName
            + ", paths=" + myPaths
            + ", parents=" + myParents
            + ", phpFiles=" + myPhpFiles
            + ", callers=" + myCallers
            + ", twigUsages=" + myTwigUsages + ")";
    }
}
