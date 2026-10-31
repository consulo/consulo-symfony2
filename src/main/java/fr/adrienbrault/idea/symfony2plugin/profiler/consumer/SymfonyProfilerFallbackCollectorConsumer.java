package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader;

import java.util.Collections;

/** Normalizes and sanitizes any profiler collector that does not have a specialized consumer. */
public final class SymfonyProfilerFallbackCollectorConsumer {
    public static final SymfonyProfilerFallbackCollectorConsumer INSTANCE = new SymfonyProfilerFallbackCollectorConsumer();

    private SymfonyProfilerFallbackCollectorConsumer() {
    }

    public SymfonyProfilerFallbackCollector read(SymfonyProfilerProfile profile, String collectorName) {
        PhpObject collector = profile.collector(collectorName);
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the '" + collectorName + "' collector");
        }
        PhpValue rawData = collector.get("data");
        ProfilerValue decoded = rawData != null
            ? new SymfonyVarDumperDataReader(profile.getResult()).read(rawData)
            : new ProfilerArray(Collections.emptyList());

        return new SymfonyProfilerFallbackCollector(
            collectorName,
            ProfilerSecretRedactor.INSTANCE.redact(decoded)
        );
    }
}
