package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A content-safe syntax or limit error at an exact byte offset.
 */
public final class PhpUnserializeException extends IllegalArgumentException {
    private final int myOffset;
    private final @Nullable Character myTag;
    private final String myReason;

    public PhpUnserializeException(int offset, @Nullable Character tag, String reason) {
        super(buildMessage(offset, tag, reason));
        myOffset = offset;
        myTag = tag;
        myReason = reason;
    }

    public int getOffset() {
        return myOffset;
    }

    public @Nullable Character getTag() {
        return myTag;
    }

    public String getReason() {
        return myReason;
    }

    private static String buildMessage(int offset, @Nullable Character tag, String reason) {
        String renderedTag;
        if (tag == null) {
            renderedTag = "";
        }
        else if (tag >= 0x21 && tag <= 0x7e) {
            renderedTag = " (tag '" + tag + "')";
        }
        else {
            String hex = Integer.toString(tag, 16).toUpperCase(Locale.ROOT);
            if (hex.length() < 2) {
                hex = "0".repeat(2 - hex.length()) + hex;
            }
            renderedTag = " (tag 0x" + hex + ")";
        }
        return "Invalid PHP serialized data at byte " + offset + renderedTag + ": " + reason;
    }
}
