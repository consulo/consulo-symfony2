package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Applies a conservative key-based safety net before unknown collector data reaches MCP output. */
public final class ProfilerSecretRedactor {
    public static final ProfilerSecretRedactor INSTANCE = new ProfilerSecretRedactor();

    private static final Set<String> SENSITIVE_EXACT_NAMES = Set.of(
        "auth",
        "key",
        "private",
        "pwd"
    );
    private static final List<String> SENSITIVE_NAME_FRAGMENTS = List.of(
        "accesskey",
        "apikey",
        "authheader",
        "authorization",
        "authentication",
        "bearer",
        "cookie",
        "credential",
        "csrf",
        "dsn",
        "encryptionkey",
        "oauth",
        "otp",
        "passphrase",
        "passwd",
        "password",
        "privatekey",
        "secret",
        "session",
        "sessionid",
        "signingkey",
        "token",
        "xsrf"
    );
    private static final Pattern CREDENTIAL_URI = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://[^/\\s:@]+:[^/\\s@]+@");

    private ProfilerSecretRedactor() {
    }

    public ProfilerValue redact(ProfilerValue value) {
        if (value instanceof ProfilerArray array) {
            List<ProfilerEntry> entries = new ArrayList<>(array.getEntries().size());
            for (ProfilerEntry entry : array.getEntries()) {
                PhpArrayKey key = entry.getKey();
                if (key instanceof PhpIntegerKey) {
                    entries.add(entry.copy(key, redact(entry.getValue())));
                }
                else {
                    String name = ((PhpStringKey) key).getBytes().utf8StringOrNull();
                    if (name == null || isSensitiveName(name)) {
                        entries.add(entry.copy(key, SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue()));
                    }
                    else {
                        entries.add(entry.copy(key, redact(entry.getValue())));
                    }
                }
            }
            return new ProfilerArray(entries);
        }
        if (value instanceof ProfilerString string) {
            return containsCredentialUri(string) ? SymfonyProfilerFallbackCollectorConsumerKt.redactedProfilerValue() : value;
        }
        return value;
    }

    private static boolean isSensitiveName(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isLetterOrDigit(character)) {
                builder.append(Character.toLowerCase(character));
            }
        }
        String normalized = builder.toString();
        if (SENSITIVE_EXACT_NAMES.contains(normalized)) {
            return true;
        }
        for (String fragment : SENSITIVE_NAME_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCredentialUri(ProfilerString value) {
        String text = value.utf8StringOrNull();
        return text != null && CREDENTIAL_URI.matcher(text).find();
    }
}
