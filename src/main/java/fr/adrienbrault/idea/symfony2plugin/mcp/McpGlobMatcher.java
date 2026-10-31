package fr.adrienbrault.idea.symfony2plugin.mcp;

import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Matches project-relative MCP paths against Ant-style glob patterns.
 *
 * Matching is performed on normalized slash-separated paths only.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class McpGlobMatcher {
    public static final McpGlobMatcher INSTANCE = new McpGlobMatcher();

    private McpGlobMatcher() {
    }

    /**
     * Matches a normalized project-relative path against a non-blank Ant-style glob.
     *
     * @param path project-relative path using "/" separators
     * @param glob Ant-style glob pattern using project-relative semantics
     */
    public boolean matches(String path, String glob) {
        String normalizedGlob = glob.trim();
        String normalizedPath = path.replace('\\', '/');

        Pattern pattern = compilePattern(normalizedGlob);
        return pattern != null && pattern.matcher(normalizedPath).matches();
    }

    private @Nullable Pattern compilePattern(String glob) {
        String normalizedGlob = glob.replace('\\', '/');

        try {
            return Pattern.compile("^" + FileUtil.convertAntToRegexp(normalizedGlob, false) + "$");
        }
        catch (PatternSyntaxException e) {
            return null;
        }
    }
}
