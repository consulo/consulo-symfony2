package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;
import org.jspecify.annotations.Nullable;

/**
 * Provides compact and collector-specific detail views for one profiler collector.
 */
public interface ProfilerDetailRenderer {
    /**
     * Collector name used by the MCP selector and availability check.
     */
    String getName();

    /**
     * Higher weights place the collector earlier in the request overview.
     */
    default int getOverviewWeight() {
        return 0;
    }

    /**
     * Renders optional content embedded in the compact request overview.
     */
    default @Nullable String renderOverview(SymfonyProfilerProfile profile) {
        return null;
    }

    /**
     * Renders the collector-specific detail view.
     */
    String renderDetails(SymfonyProfilerProfile profile, int page);
}
