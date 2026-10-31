package fr.adrienbrault.idea.symfony2plugin.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import fr.adrienbrault.idea.symfony2plugin.mcp.toolset.SymfonyToolset;

/**
 * MCP Tools Provider for Symfony Plugin.
 * <p>
 * The tools are always registered; the platform MCP server settings control whether the server runs and which tools
 * are exposed. Each tool fails with an error when Symfony is not enabled in the project.
 * <p>
 * Note: Consulo has no reflection over annotated tool methods; {@link SymfonyToolset} registers
 * each tool explicitly. The tool name is the upstream method name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class SymfonyMcpToolsProvider implements McpToolset {
    private static final Logger LOG = Logger.getInstance(SymfonyMcpToolsProvider.class);

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        try {
            new SymfonyToolset().registerTools(registrar);
        }
        catch (Exception e) {
            LOG.error("Error loading tools from SymfonyToolset", e);
        }
    }
}
