package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.util.io.FileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared path normalization and scope classification logic for {@link SymfonyVarDirectoryWatcher}.
 * <p>
 * Keeps VFS event matching deterministic across platforms by normalizing separators,
 * resolving {@code .} / {@code ..}, and folding Windows drive letters to a stable lowercase form.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class SymfonyVarDirectoryWatchScopeUtil {
    public static final SymfonyVarDirectoryWatchScopeUtil INSTANCE = new SymfonyVarDirectoryWatchScopeUtil();

    private SymfonyVarDirectoryWatchScopeUtil() {
    }

    /**
     * Self-contained watch scope definition with normalized roots and an optional file predicate.
     */
    public static final class ScopeEntry {
        private final SymfonyVarDirectoryWatcher.Scope myScope;
        private final Set<String> myRoots;
        private final @Nullable Predicate<String> myPredicate;

        // consulo-port: Kotlin function type ((String) -> Boolean)? is exposed as java.util.function.Predicate
        public ScopeEntry(SymfonyVarDirectoryWatcher.Scope scope, Set<String> roots, @Nullable Predicate<String> predicate) {
            myScope = scope;
            myRoots = roots;
            myPredicate = predicate;
        }

        public SymfonyVarDirectoryWatcher.Scope getScope() {
            return myScope;
        }

        public Set<String> getRoots() {
            return myRoots;
        }

        public @Nullable Predicate<String> getPredicate() {
            return myPredicate;
        }

        public SymfonyVarDirectoryWatcher.Scope component1() {
            return myScope;
        }

        public Set<String> component2() {
            return myRoots;
        }

        public @Nullable Predicate<String> component3() {
            return myPredicate;
        }

        public ScopeEntry copy(SymfonyVarDirectoryWatcher.Scope scope, Set<String> roots, @Nullable Predicate<String> predicate) {
            return new ScopeEntry(scope, roots, predicate);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ScopeEntry that)) {
                return false;
            }
            return myScope == that.myScope
                && Objects.equals(myRoots, that.myRoots)
                && Objects.equals(myPredicate, that.myPredicate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myScope, myRoots, myPredicate);
        }

        @Override
        public String toString() {
            return "ScopeEntry(scope=" + myScope + ", roots=" + myRoots + ", predicate=" + myPredicate + ")";
        }
    }

    /**
     * Builds the watcher scopes from the current project base path and plugin settings.
     * <p>
     * Directory-like roots are stored with a trailing slash while configured file roots
     * remain plain normalized file paths.
     */
    public List<ScopeEntry> createScopes(String base, Settings settings) {
        List<String> containerFilePaths = new ArrayList<>();
        if (settings.containerFiles != null) {
            for (ContainerFile containerFile : settings.containerFiles) {
                String path = containerFile.getPath();
                if (path != null && !path.isBlank()) {
                    containerFilePaths.add(path);
                }
            }
        }

        Set<String> containerRoots = new LinkedHashSet<>();
        containerRoots.add(normalizeRootPath(base + "/var/cache"));
        containerRoots.add(normalizeRootPath(base + "/app/cache"));
        for (String path : containerFilePaths) {
            containerRoots.add(normalizePath(resolve(base, path)));
        }

        Set<String> explicitTranslationRoots = new LinkedHashSet<>();
        for (String path : containerFilePaths) {
            String normalizedContainerPath = resolve(base, path);
            int index = normalizedContainerPath.lastIndexOf('/');
            String parent = index < 0 ? "" : normalizedContainerPath.substring(0, index);
            if (!parent.isBlank()) {
                explicitTranslationRoots.add(normalizeRootPath(parent + "/translations"));
            }
        }

        String configuredPath = settings.pathToTranslation;
        if (configuredPath != null && !configuredPath.isBlank()) {
            explicitTranslationRoots.add(normalizeRootPath(resolve(base, configuredPath)));
        }

        Set<String> translationRoots = new LinkedHashSet<>();
        translationRoots.add(normalizeRootPath(base + "/var/cache"));
        translationRoots.add(normalizeRootPath(base + "/app/cache"));
        translationRoots.addAll(explicitTranslationRoots);

        Set<String> routeRoots = new LinkedHashSet<>();
        for (String route : Settings.DEFAULT_ROUTES) {
            routeRoots.add(normalizePath(resolve(base, route)));
        }
        if (settings.routingFiles != null) {
            for (RoutingFile routingFile : settings.routingFiles) {
                String path = routingFile.getPath();
                if (path != null && !path.isBlank()) {
                    routeRoots.add(normalizePath(resolve(base, path)));
                }
            }
        }

        List<ScopeEntry> scopes = new ArrayList<>();
        scopes.add(new ScopeEntry(
            SymfonyVarDirectoryWatcher.Scope.CONTAINER,
            containerRoots,
            // Container scope only reacts to compiled container XML files in dev/dev_* cache directories.
            this::isCompiledContainerPath
        ));
        scopes.add(new ScopeEntry(
            SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS,
            translationRoots,
            path -> isTranslationPath(path, explicitTranslationRoots)
        ));
        scopes.add(new ScopeEntry(
            SymfonyVarDirectoryWatcher.Scope.ROUTES,
            routeRoots,
            this::isCompiledRoutePath
        ));

        return scopes;
    }

    private String resolve(String base, String path) {
        String normalized = normalizePath(path);
        return isAbsolutePath(normalized) ? normalized : normalizePath(base + "/" + normalized);
    }

    /**
     * Returns the scopes that should be invalidated for the given event paths.
     * <p>
     * Paths are normalized first so callers can pass raw VFS event paths directly.
     */
    public Set<SymfonyVarDirectoryWatcher.Scope> collectScopesForPaths(List<ScopeEntry> scopes, List<String> paths) {
        List<String> normalizedPaths = new ArrayList<>();
        for (String path : paths) {
            normalizedPaths.add(normalizePath(path));
        }

        Set<SymfonyVarDirectoryWatcher.Scope> toInvalidate = new LinkedHashSet<>();

        for (ScopeEntry entry : scopes) {
            if (toInvalidate.contains(entry.getScope())) {
                continue;
            }

            Predicate<String> predicate = entry.getPredicate();
            if (normalizedPaths.stream().anyMatch(path -> entry.getRoots().stream().anyMatch(root -> isRelevantPath(path, root)))
                && (predicate == null || normalizedPaths.stream().anyMatch(predicate))) {
                toInvalidate.add(entry.getScope());
            }
        }

        return toInvalidate;
    }

    /**
     * Returns true for translation-related paths discovered from cache conventions or explicit roots.
     */
    public boolean isTranslationPath(String path, Set<String> explicitRoots) {
        String normalized = normalizePath(path);

        if (explicitRoots.stream().anyMatch(root -> isRelevantPath(normalized, root))) {
            return true;
        }

        return isCompiledTranslationCachePath(normalized);
    }

    /**
     * Returns true for cache paths that can affect compiled translations in {@code dev} or {@code dev_*}.
     */
    public boolean isCompiledTranslationCachePath(String path) {
        String lowered = normalizePath(path).toLowerCase(Locale.ROOT);

        for (String cacheRoot : new String[]{"/var/cache", "/app/cache"}) {
            int index = lowered.indexOf(cacheRoot);
            if (index < 0) {
                continue;
            }

            String remainder = trimStartSlashes(lowered.substring(index + cacheRoot.length()));
            if (remainder.isEmpty()) {
                return true;
            }

            // keep trailing empty segments like Kotlin's split
            String[] segments = remainder.split("/", -1);
            String devSegment = segments[0];
            if (!devSegment.equals("dev") && !devSegment.startsWith("dev_")) {
                continue;
            }

            if (segments.length == 1 || segments[1].equals("translations")) {
                return true;
            }
        }

        return false;
    }

    private static String trimStartSlashes(String value) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return value.substring(start);
    }

    /**
     * Returns true when {@code path} is inside a Symfony {@code var/cache} or {@code app/cache} directory.
     */
    public boolean isCachePath(String path) {
        String lowered = normalizePath(path).toLowerCase(Locale.ROOT);
        return lowered.contains("/var/cache") || lowered.contains("/app/cache");
    }

    /**
     * Returns true for compiled Symfony route PHP files.
     * <p>
     * Matches the URL-generator files produced by {@code RouteHelper.getDefaultRoutes}:
     * {@code appDevUrlGenerator.php}, {@code url_generating_routes.php}, {@code UrlGenerator.php}, etc.
     * <p>
     * Callers are expected to pre-filter paths with {@link #isCachePath} before invoking this.
     */
    public boolean isCompiledRoutePath(String path) {
        String normalized = normalizePath(path);
        if (!normalized.endsWith(".php")) {
            return false;
        }
        String filename = substringAfterLastSlash(normalized.toLowerCase(Locale.ROOT)).replace("_", "");
        return filename.contains("urlgenerator") || filename.contains("urlgenerating");
    }

    /**
     * Returns true for compiled Symfony container XML files inside {@code dev} or {@code dev_*} cache directories.
     * <p>
     * Callers are expected to pre-filter paths with {@link #isCachePath} before invoking this.
     */
    public boolean isCompiledContainerPath(String path) {
        String normalized = normalizePath(path);
        if (!normalized.endsWith(".xml")) {
            return false;
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        // `contains` intentionally matches both `dev` and `dev_*` (e.g. `dev_392373729`) cache directories.
        if (!lowered.contains("/var/cache/dev") && !lowered.contains("/app/cache/dev")) {
            return false;
        }

        String filename = substringAfterLastSlash(lowered);
        return filename.contains("debugcontainer")
            || (filename.contains("debug") && filename.contains("container"))
            || (filename.contains("kernel") && filename.contains("container"));
    }

    private static String substringAfterLastSlash(String value) {
        int index = value.lastIndexOf('/');
        return index < 0 ? value : value.substring(index + 1);
    }

    /**
     * Returns true when {@code path} is inside {@code root} or when {@code path} is a parent of {@code root}.
     * <p>
     * The second case is needed for rename/delete events affecting a watched directory
     * through one of its ancestors.
     */
    public boolean isRelevantPath(String path, String root) {
        String normalizedPath = normalizePath(path);
        String normalizedRoot = normalizePath(root);

        return FileUtil.startsWith(normalizedPath, normalizedRoot) || FileUtil.startsWith(normalizedRoot, normalizedPath);
    }

    /**
     * Normalizes a directory-like root and ensures it ends with {@code /}.
     */
    public String normalizeRootPath(String path) {
        String normalized = normalizePath(path);
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /**
     * Cross-platform absolute-path detection for normalized VFS-style paths.
     * <p>
     * Supports Unix roots, UNC-style roots, and Windows drive-letter paths independently
     * of the host OS running the IDE or the tests.
     */
    public boolean isAbsolutePath(String path) {
        String normalized = normalizePath(path);
        return normalized.startsWith("/")
            || normalized.startsWith("//")
            || (normalized.length() >= 3 && normalized.charAt(1) == ':' && normalized.charAt(2) == '/');
    }

    /**
     * Converts a path to a canonical VFS-style form.
     * <p>
     * This normalizes separators, resolves {@code .} and {@code ..}, and folds Windows drive letters
     * to lowercase so string-based matching remains stable across event sources.
     */
    public String normalizePath(String path) {
        String canonical = Objects.requireNonNull(FileUtil.toCanonicalPath(FileUtil.toSystemIndependentName(path)));

        if (canonical.length() >= 3 && canonical.charAt(1) == ':' && canonical.charAt(2) == '/') {
            return Character.toLowerCase(canonical.charAt(0)) + canonical.substring(1);
        }

        return canonical;
    }
}
