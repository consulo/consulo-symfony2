package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValueKt;

public final class SymfonyProfilerFallbackCollectorConsumerKt {
    public static final String PROFILER_REDACTED_TEXT = "***REDACTED***";

    private SymfonyProfilerFallbackCollectorConsumerKt() {
    }

    public static ProfilerString redactedProfilerValue() {
        return ProfilerValueKt.profilerString(PROFILER_REDACTED_TEXT);
    }
}
