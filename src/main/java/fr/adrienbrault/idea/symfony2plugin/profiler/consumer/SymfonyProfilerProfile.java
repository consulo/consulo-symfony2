package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeException;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeLimits;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValueKt;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Parsed raw Symfony profile shared by the individual profiler collector consumers.
 *
 * Keeping the PHP parse in this neutral model lets MCP expose more collector types later without
 * coupling profile loading or request metadata to Doctrine's `db` collector.
 */
public final class SymfonyProfilerProfile {
    private static final int PROFILER_MAX_INPUT_BYTES = 25 * 1024 * 1024;

    public static final Companion Companion = new Companion();

    private final PhpUnserializeResult myResult;
    private final PhpArray myProfile;

    private SymfonyProfilerProfile(PhpUnserializeResult result, PhpArray profile) {
        myResult = result;
        myProfile = profile;
    }

    public PhpUnserializeResult getResult() {
        return myResult;
    }

    public @Nullable String getToken() {
        return utf8StringOrNull(SymfonyProfilerProfileKt.resolve(myProfile.get("token"), myResult));
    }

    public @Nullable String getMethod() {
        return utf8StringOrNull(SymfonyProfilerProfileKt.resolve(myProfile.get("method"), myResult));
    }

    public @Nullable String getUrl() {
        return utf8StringOrNull(SymfonyProfilerProfileKt.resolve(myProfile.get("url"), myResult));
    }

    public @Nullable Integer getStatusCode() {
        return SymfonyProfilerProfileKt.resolve(myProfile.get("status_code"), myResult) instanceof PhpInteger integer
            ? (int) integer.getValue()
            : null;
    }

    public @Nullable Long getTime() {
        return SymfonyProfilerProfileKt.resolve(myProfile.get("time"), myResult) instanceof PhpInteger integer
            ? integer.getValue()
            : null;
    }

    public List<String> getCollectorNames() {
        List<String> names = new ArrayList<>();
        for (PhpArrayEntry entry : collectors().getEntries()) {
            if (entry.getKey() instanceof PhpStringKey stringKey) {
                String name = stringKey.getBytes().utf8StringOrNull();
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public @Nullable PhpObject collector(String name) {
        return SymfonyProfilerProfileKt.resolve(collectors().get(name), myResult) instanceof PhpObject object ? object : null;
    }

    private PhpArray collectors() {
        if (SymfonyProfilerProfileKt.resolve(myProfile.get("data"), myResult) instanceof PhpArray array) {
            return array;
        }
        throw new IllegalStateException("Symfony profile does not contain a collector array");
    }

    private static @Nullable String utf8StringOrNull(@Nullable PhpValue value) {
        return value != null ? PhpValueKt.utf8StringOrNull(value) : null;
    }

    public static final class Companion {
        private Companion() {
        }

        /** Parses raw or GZIP data with fixed defensive limits before collector consumers run. */
        public SymfonyProfilerProfile read(byte[] input) {
            PhpUnserializeLimits defaults = new PhpUnserializeLimits();
            PhpUnserializeLimits limits = new PhpUnserializeLimits(
                PROFILER_MAX_INPUT_BYTES,
                defaults.getMaxNestingDepth(),
                defaults.getMaxTotalValues(),
                defaults.getMaxContainerEntries(),
                defaults.getMaxPayloadBytes()
            );
            if (input.length > limits.getMaxInputBytes()) {
                throw new PhpUnserializeException(
                    limits.getMaxInputBytes(),
                    null,
                    "input exceeds limit " + limits.getMaxInputBytes()
                );
            }

            byte[] serialized = decodeGzip(input, limits.getMaxInputBytes());
            PhpUnserializeResult result = PhpUnserializer.unserialize(serialized, limits);
            if (!(result.getRoot() instanceof PhpArray profile)) {
                throw new IllegalStateException("Symfony profile root must be a PHP array");
            }
            if (!(SymfonyProfilerProfileKt.resolve(profile.get("data"), result) instanceof PhpArray)) {
                throw new IllegalStateException("Symfony profile does not contain a collector array");
            }

            return new SymfonyProfilerProfile(result, profile);
        }

        private byte[] decodeGzip(byte[] input, int maxBytes) {
            if (input.length < 3 || input[0] != (byte) 0x1f || input[1] != (byte) 0x8b || input[2] != (byte) 8) {
                return input;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(input.length, maxBytes));
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(input))) {
                byte[] buffer = new byte[8 * 1024];
                int total = 0;
                while (true) {
                    int read = gzip.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    if (read > maxBytes - total) {
                        throw new PhpUnserializeException(
                            maxBytes,
                            null,
                            "decompressed input exceeds limit " + maxBytes
                        );
                    }
                    output.write(buffer, 0, read);
                    total += read;
                }
            }
            catch (IOException e) {
                // Kotlin propagated the IOException unchecked
                throw new UncheckedIOException(e);
            }

            return output.toByteArray();
        }
    }
}
