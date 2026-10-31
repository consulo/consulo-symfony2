package fr.adrienbrault.idea.symfony2plugin.mcp;

public final class McpCsvUtil {
    public static final McpCsvUtil INSTANCE = new McpCsvUtil();

    private McpCsvUtil() {
    }

    public String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        else {
            return value;
        }
    }
}
