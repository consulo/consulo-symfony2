package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValueKt;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/** Keeps the request-specific security policies separate from the generic value decoder. */
public final class SymfonyProfilerRequestSanitizer {
    public static final SymfonyProfilerRequestSanitizer INSTANCE = new SymfonyProfilerRequestSanitizer();

    private static final String REDACTED_BODY =
        SymfonyProfilerFallbackCollectorConsumerKt.PROFILER_REDACTED_TEXT + " (raw request body omitted; see request_request / request_query)";
    private static final String REDACTED_CURL =
        SymfonyProfilerFallbackCollectorConsumerKt.PROFILER_REDACTED_TEXT
            + " (curl command omitted; reconstructs the raw body, URL query string and request headers)";

    private static final List<String> SENSITIVE_ENV_PATTERNS = List.of(
        "SECRET",
        "KEY",
        "PASSWORD",
        "TOKEN",
        "BEARER",
        "AUTH",
        "CREDENTIAL",
        "PRIVATE",
        "COOKIE",
        "DSN"
    );
    private static final Set<String> SENSITIVE_SERVER_KEYS = Set.of("REQUEST_URI", "QUERY_STRING");
    private static final List<String> SENSITIVE_HEADER_PATTERNS = List.of(
        "authorization",
        "cookie",
        "auth",
        "token",
        "secret",
        "credential",
        "csrf",
        "xsrf",
        "api-key",
        "apikey",
        "session",
        "x-amz-security"
    );
    private static final List<String> SENSITIVE_PARAM_PATTERNS = List.of(
        "PASSWORD",
        "PASSWD",
        "PASSPHRASE",
        "PWD",
        "SECRET",
        "TOKEN",
        "API_KEY",
        "APIKEY",
        "ACCESS_KEY",
        "SIGNING_KEY",
        "ENCRYPTION_KEY",
        "OAUTH",
        "CREDENTIAL",
        "PRIVATE",
        "BEARER",
        "CSRF",
        "XSRF",
        "OTP"
    );
    private static final Pattern CREDENTIAL_URI = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://[^/\\s:@]+:[^/\\s@]+@");

    private SymfonyProfilerRequestSanitizer() {
    }

    public ProfilerArray sanitize(ProfilerArray data) {
        return transformEntries(data, this::sanitizeEntry);
    }

    private ProfilerValue sanitizeEntry(@Nullable String name, ProfilerValue value) {
        if (name == null) {
            return redactSensitiveParams(value);
        }

        return switch (name) {
            case "request_cookies", "response_cookies", "session_attributes" -> redactImmediateValues(value);
            case "request_headers", "response_headers" -> sanitizeHeaders(value);
            case "request_server" -> sanitizeServer(value);
            case "dotenv_vars" -> sanitizeEnvironment(value);
            case "request_request", "request_query", "request_attributes" -> redactSensitiveParams(value);
            case "content" -> hasContent(value) ? ProfilerValueKt.profilerString(REDACTED_BODY) : value;
            case "curlCommand" -> !(value instanceof ProfilerNull) ? ProfilerValueKt.profilerString(REDACTED_CURL) : value;
            default -> redactSensitiveParams(value);
        };
    }

    private ProfilerValue redactImmediateValues(ProfilerValue value) {
        if (value instanceof ProfilerArray array) {
            List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
            for (ProfilerEntry entry : array.getEntries()) {
                entries.add(entry.copy(entry.getKey(), SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue()));
            }
            return new ProfilerArray(entries);
        }
        return SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue();
    }

    private ProfilerValue sanitizeHeaders(ProfilerValue value) {
        return transformArray(value, entry -> {
            String name = stringKeyOrNull(entry);
            if (name == null || SENSITIVE_HEADER_PATTERNS.stream().anyMatch(pattern -> StringUtils.containsIgnoreCase(name, pattern))) {
                return entry.copy(entry.getKey(), SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue());
            }
            return entry;
        });
    }

    private ProfilerValue sanitizeEnvironment(ProfilerValue value) {
        return transformArray(value, entry -> {
            String name = stringKeyOrNull(entry);
            if (name == null || isSensitiveEnvironmentName(name) || containsCredentialUri(entry.getValue())) {
                return entry.copy(entry.getKey(), SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue());
            }
            return entry.copy(entry.getKey(), redactSensitiveParams(entry.getValue()));
        });
    }

    private ProfilerValue sanitizeServer(ProfilerValue value) {
        ProfilerValue environment = sanitizeEnvironment(value);
        return transformArray(environment, entry -> {
            String name = stringKeyOrNull(entry);
            if (name != null && SENSITIVE_SERVER_KEYS.contains(name.toUpperCase(Locale.ROOT)) && hasContent(entry.getValue())) {
                return entry.copy(entry.getKey(), SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue());
            }
            return entry;
        });
    }

    private ProfilerValue redactSensitiveParams(ProfilerValue value) {
        if (!(value instanceof ProfilerArray array)) {
            return value;
        }

        List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
        for (ProfilerEntry entry : array.getEntries()) {
            if (entry.getKey() instanceof PhpIntegerKey) {
                entries.add(entry.copy(entry.getKey(), redactSensitiveParams(entry.getValue())));
            }
            else {
                // PhpArrayKey is sealed: the remaining kind is PhpStringKey
                String name = stringKeyOrNull(entry);
                if (name == null || isSensitiveParameterName(name)) {
                    entries.add(entry.copy(entry.getKey(), SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue()));
                }
                else {
                    entries.add(entry.copy(entry.getKey(), redactSensitiveParams(entry.getValue())));
                }
            }
        }
        return new ProfilerArray(entries);
    }

    private static boolean isSensitiveEnvironmentName(String name) {
        String uppercase = name.toUpperCase(Locale.ROOT);
        for (String pattern : SENSITIVE_ENV_PATTERNS) {
            if (uppercase.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSensitiveParameterName(String name) {
        String uppercase = name.toUpperCase(Locale.ROOT);
        for (String pattern : SENSITIVE_PARAM_PATTERNS) {
            if (uppercase.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCredentialUri(ProfilerValue value) {
        String text = value instanceof ProfilerString string ? string.utf8StringOrNull() : null;
        return text != null && CREDENTIAL_URI.matcher(text).find();
    }

    private static boolean hasContent(ProfilerValue value) {
        if (value instanceof ProfilerString string) {
            return string.getBytes().getSize() > 0;
        }
        return !(value instanceof ProfilerNull);
    }

    private static ProfilerValue transformArray(ProfilerValue value, UnaryOperator<ProfilerEntry> transform) {
        if (!(value instanceof ProfilerArray array)) {
            return value;
        }
        List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
        for (ProfilerEntry entry : array.getEntries()) {
            entries.add(transform.apply(entry));
        }
        return new ProfilerArray(entries);
    }

    private static ProfilerArray transformEntries(
        ProfilerArray array,
        BiFunction<@Nullable String, ProfilerValue, ProfilerValue> transform
    ) {
        List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
        for (ProfilerEntry entry : array.getEntries()) {
            entries.add(entry.copy(entry.getKey(), transform.apply(stringKeyOrNull(entry), entry.getValue())));
        }
        return new ProfilerArray(entries);
    }

    private static @Nullable String stringKeyOrNull(ProfilerEntry entry) {
        return entry.getKey() instanceof PhpStringKey stringKey ? stringKey.getBytes().utf8StringOrNull() : null;
    }
}
