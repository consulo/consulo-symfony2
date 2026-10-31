package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerBoolean;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEnum;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerFloat;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerOpaque;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerReference;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Renders collector-neutral values as bounded indented key/value text with recognized lists.
 */
public final class ProfilerTextRenderer {
    public static final ProfilerTextRenderer INSTANCE = new ProfilerTextRenderer();

    private static final int MAX_DEPTH = 32;
    private static final int MAX_LINES = 10_000;
    private static final int MAX_KEY_LENGTH = 200;
    private static final int MAX_SCALAR_LENGTH = 1_000;
    private static final Set<Character> MARKDOWN_LEADING_CHARACTERS = Set.of('#', '-', '*', '+', '>', '|', '`');
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private ProfilerTextRenderer() {
    }

    public List<String> render(ProfilerValue value, int initialIndent) {
        RenderState state = new RenderState();
        if (value instanceof ProfilerArray array) {
            if (array.getEntries().isEmpty()) {
                state.add(" ".repeat(initialIndent) + "(empty)");
            }
            else {
                state.appendArray(array, initialIndent, 0);
            }
        }
        else {
            state.add(" ".repeat(initialIndent) + "Value: " + scalar(value));
        }

        if (state.truncated) {
            if (state.lines.size() == MAX_LINES) {
                state.lines.remove(state.lines.size() - 1);
            }
            state.lines.add(" ".repeat(initialIndent) + "[output truncated]");
        }
        return state.lines;
    }

    /**
     * Kotlin default argument {@code initialIndent = 0}.
     */
    public List<String> render(ProfilerValue value) {
        return render(value, 0);
    }

    /**
     * Produces one single-line CSV record from untrusted profiler values.
     */
    public String csvRow(String... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            String normalized = CONTROL_CHARACTERS.matcher(values[i]).replaceAll(" ");
            if (normalized.indexOf(',') >= 0 || normalized.indexOf('"') >= 0) {
                builder.append('"').append(normalized.replace("\"", "\"\"")).append('"');
            }
            else {
                builder.append(normalized);
            }
        }
        return builder.toString();
    }

    public @Nullable String formatTimestamp(long epochSeconds) {
        try {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault())
            );
        }
        catch (Throwable e) {
            return null;
        }
    }

    private static class RenderState {
        final List<String> lines = new ArrayList<>();
        boolean truncated = false;

        void appendArray(ProfilerArray value, int indent, int depth) {
            if (depth >= MAX_DEPTH) {
                add(" ".repeat(indent) + "[maximum depth reached]");
                return;
            }

            if (isList(value)) {
                appendList(value, indent, depth);
                return;
            }

            for (ProfilerEntry entry : value.getEntries()) {
                if (truncated) {
                    return;
                }
                String prefix = " ".repeat(indent) + key(entry.getKey());
                ProfilerArray nested = entry.getValue() instanceof ProfilerArray array ? array : null;
                if (nested == null) {
                    add(prefix + ": " + scalar(entry.getValue()));
                }
                else if (nested.getEntries().isEmpty()) {
                    add(prefix + ": (empty)");
                }
                else {
                    add(prefix + ":");
                    appendArray(nested, indent + 2, depth + 1);
                }
            }
        }

        private void appendList(ProfilerArray value, int indent, int depth) {
            for (ProfilerEntry entry : value.getEntries()) {
                if (truncated) {
                    return;
                }
                String prefix = " ".repeat(indent) + "-";
                ProfilerArray nested = entry.getValue() instanceof ProfilerArray array ? array : null;
                if (nested == null) {
                    add(prefix + " " + scalar(entry.getValue()));
                }
                else if (nested.getEntries().isEmpty()) {
                    add(prefix + " (empty)");
                }
                else {
                    add(prefix);
                    appendArray(nested, indent + 2, depth + 1);
                }
            }
        }

        void add(String line) {
            if (lines.size() >= MAX_LINES) {
                truncated = true;
                return;
            }
            lines.add(line);
        }
    }

    private static boolean isList(ProfilerArray array) {
        List<ProfilerEntry> entries = array.getEntries();
        if (entries.isEmpty()) {
            return false;
        }

        for (int index = 0; index < entries.size(); index++) {
            if (!(entries.get(index).getKey() instanceof PhpIntegerKey integerKey) || integerKey.getValue() != (long) index) {
                return false;
            }
        }
        return true;
    }

    private static String key(PhpArrayKey key) {
        if (key instanceof PhpIntegerKey integerKey) {
            return "[" + integerKey.getValue() + "]";
        }
        else if (key instanceof PhpStringKey stringKey) {
            String text = stringKey.getBytes().utf8StringOrNull();
            if (text == null) {
                return "[binary key, " + stringKey.getBytes().getSize() + " bytes]";
            }
            String escaped = escape(text, MAX_KEY_LENGTH);
            // Prefix syntax-looking keys so untrusted values cannot create Markdown blocks.
            return !escaped.isEmpty() && MARKDOWN_LEADING_CHARACTERS.contains(escaped.charAt(0)) ? "key " + escaped : escaped;
        }

        throw new IllegalStateException("Unknown PHP array key: " + key);
    }

    private static String scalar(ProfilerValue value) {
        if (value instanceof ProfilerNull) {
            return "null";
        }
        else if (value instanceof ProfilerBoolean booleanValue) {
            return String.valueOf(booleanValue.getValue());
        }
        else if (value instanceof ProfilerInteger integerValue) {
            return String.valueOf(integerValue.getValue());
        }
        else if (value instanceof ProfilerFloat floatValue) {
            return String.valueOf(floatValue.getValue());
        }
        else if (value instanceof ProfilerString stringValue) {
            String text = stringValue.utf8StringOrNull();
            return text != null ? escape(text, MAX_SCALAR_LENGTH) : "[binary string, " + stringValue.getBytes().getSize() + " bytes]";
        }
        else if (value instanceof ProfilerArray arrayValue) {
            return arrayValue.getEntries().isEmpty() ? "(empty)" : "[nested value]";
        }
        else if (value instanceof ProfilerEnum enumValue) {
            String enumName = enumValue.getEnumName().utf8StringOrNull();
            if (enumName == null) {
                enumName = "binary enum";
            }
            String caseName = enumValue.getCaseName().utf8StringOrNull();
            if (caseName == null) {
                caseName = "binary case";
            }
            return escape(enumName + "::" + caseName, MAX_SCALAR_LENGTH);
        }
        else if (value instanceof ProfilerReference referenceValue) {
            return "[reference to position " + referenceValue.getPosition() + "]";
        }
        else if (value instanceof ProfilerOpaque opaqueValue) {
            StringBuilder builder = new StringBuilder();
            builder.append("[opaque ").append(escape(opaqueValue.getKind(), MAX_KEY_LENGTH));
            Integer payloadBytes = opaqueValue.getPayloadBytes();
            if (payloadBytes != null) {
                builder.append(", ").append(payloadBytes).append(" bytes");
            }
            builder.append(']');
            return builder.toString();
        }

        throw new IllegalStateException("Unknown profiler value: " + value);
    }

    /**
     * Escapes line-breaking controls before truncation so one value always occupies one line.
     */
    private static String escape(String value, int maxLength) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\0' -> escaped.append("\\0");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20 || character == 0x7f) {
                        escaped.append("\\u");
                        escaped.append(String.format("%04x", (int) character));
                    }
                    else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.length() <= maxLength ? escaped.toString() : escaped.substring(0, maxLength) + "… [truncated]";
    }
}
