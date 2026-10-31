package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/** Reads and sanitizes the Symfony request collector without fixing its complete field schema. */
public final class SymfonyProfilerRequestConsumer {
    public static final SymfonyProfilerRequestConsumer INSTANCE = new SymfonyProfilerRequestConsumer();

    private static final Set<String> REQUEST_COLLECTOR_CLASSES = Set.of(
        "Symfony\\Component\\HttpKernel\\DataCollector\\RequestDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RequestDataCollector"
    );

    private SymfonyProfilerRequestConsumer() {
    }

    public SymfonyProfilerRequest read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("request");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the request collector");
        }
        String className = collector.getClassName().utf8StringOrNull();
        if (className == null || !REQUEST_COLLECTOR_CLASSES.contains(className)) {
            throw new IllegalStateException("Symfony request collector has an unsupported class");
        }

        PhpValue rawData = collector.get("data");
        if (rawData == null) {
            throw new IllegalStateException("Symfony request collector does not contain its data value");
        }
        if (!(new SymfonyVarDumperDataReader(profile.getResult()).read(rawData) instanceof ProfilerArray decoded)) {
            throw new IllegalStateException("Symfony request collector data is not an array");
        }

        // Sanitization happens before the value tree can reach any output formatter.
        ProfilerArray sanitized = SymfonyProfilerRequestSanitizer.INSTANCE.sanitize(decoded);

        Integer statusCode = null;
        if (sanitized.get("status_code") instanceof ProfilerInteger statusCodeValue
            && statusCodeValue.getValue() >= Integer.MIN_VALUE && statusCodeValue.getValue() <= Integer.MAX_VALUE) {
            statusCode = (int) statusCodeValue.getValue();
        }

        return new SymfonyProfilerRequest(
            sanitized,
            new SymfonyProfilerRequestSummary(
                stringValue(sanitized, "method"),
                stringValue(sanitized, "path_info"),
                stringValue(sanitized, "route"),
                statusCode,
                stringValue(sanitized, "content_type")
            )
        );
    }

    private static @Nullable String stringValue(ProfilerArray array, String key) {
        return array.get(key) instanceof ProfilerString string ? string.utf8StringOrNull() : null;
    }
}
