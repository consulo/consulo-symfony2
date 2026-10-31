package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import consulo.mcp.tool.McpToolException;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerRendererException;
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerTextRenderer;
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerRequestDetailsRenderer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Loads one profiler request and delegates its presentation to profiler renderers.
 */
public final class SymfonyProfilerRequestDetailsCollector {
    private final ProfilerIndexInterface myProfilerIndex;

    public SymfonyProfilerRequestDetailsCollector(ProfilerIndexInterface profilerIndex) {
        myProfilerIndex = profilerIndex;
    }

    /**
     * @param hash      exact hexadecimal profiler token or `latest` (newest known request)
     * @param collector optional collector name; `null` renders the compact overview
     * @param page      1-based page for the paginated collector detail view
     */
    public String collect(String hash, @Nullable String collector, int page) {
        String normalizedHash = hash.trim();
        Supplier<List<ProfilerRequestInterface>> recentRequests = new Supplier<>() {
            private @Nullable List<ProfilerRequestInterface> myValue;

            @Override
            public List<ProfilerRequestInterface> get() {
                if (myValue == null) {
                    myValue = myProfilerIndex.getRequests();
                }
                return myValue;
            }
        };

        String resolvedHash;
        if (normalizedHash.equals("latest")) {
            List<ProfilerRequestInterface> requests = recentRequests.get();
            if (requests.isEmpty()) {
                throw new McpToolException("No profiler requests are available to resolve 'latest'.");
            }
            resolvedHash = requests.get(0).getHash();
        }
        else {
            resolvedHash = normalizedHash;
        }

        if (!resolvedHash.matches("^[a-fA-F0-9]{6,64}$")) {
            throw new McpToolException("hash must be 'latest' or a 6-64 character hexadecimal profiler token.");
        }

        String requestedCollector = null;
        if (collector != null && !collector.trim().isEmpty()) {
            requestedCollector = collector.trim();
        }

        byte[] rawProfile = myProfilerIndex.getRawProfile(resolvedHash);
        if (rawProfile == null) {
            throw new McpToolException(
                "Raw profiler data for '" + resolvedHash + "' is not available. " +
                    "Profiler details currently require a local Symfony profiler file."
            );
        }

        SymfonyProfilerProfile profile;
        try {
            profile = SymfonyProfilerProfile.Companion.read(rawProfile);
        }
        catch (Exception exception) {
            String newestHashes = recentRequests.get()
                .stream()
                .limit(10)
                .map(request -> {
                    Long time = request.getTime();
                    String profiledAt = time != null ? ProfilerTextRenderer.INSTANCE.formatTimestamp(time) : null;
                    return profiledAt != null ? request.getHash() + " (" + profiledAt + ")" : request.getHash();
                })
                .collect(Collectors.joining(", "));
            if (newestHashes.isEmpty()) {
                newestHashes = "(none)";
            }
            throw new McpToolException(
                "Unable to parse profiler request '" + resolvedHash + "': " + exception.getMessage() + ". " +
                    "Newest profiler hashes: " + newestHashes
            );
        }

        if (requestedCollector != null && !profile.getCollectorNames().contains(requestedCollector)) {
            String availableCollectors = String.join(", ", profile.getCollectorNames());
            if (availableCollectors.isEmpty()) {
                availableCollectors = "(none)";
            }
            throw new McpToolException(
                "Profiler request '" + resolvedHash + "' does not contain the '" + requestedCollector + "' collector. " +
                    "Available collectors: " + availableCollectors
            );
        }

        try {
            return SymfonyProfilerRequestDetailsRenderer.INSTANCE.render(profile, resolvedHash, requestedCollector, page);
        }
        catch (ProfilerRendererException exception) {
            throw new McpToolException(
                "Unable to read the '" + exception.getCollectorName() + "' collector for '" + resolvedHash + "': " +
                    exception.getMessage()
            );
        }
    }
}
