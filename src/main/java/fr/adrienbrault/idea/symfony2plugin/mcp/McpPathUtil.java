package fr.adrienbrault.idea.symfony2plugin.mcp;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;

public final class McpPathUtil {
    public static final McpPathUtil INSTANCE = new McpPathUtil();

    private McpPathUtil() {
    }

    public String getRelativeProjectPath(Project project, VirtualFile virtualFile) {
        String relativePath = VfsExUtil.getRelativeProjectPathStrict(project, virtualFile);
        if (relativePath != null) {
            return relativePath;
        }

        relativePath = VfsExUtil.getRelativeProjectPath(project, virtualFile);
        return relativePath != null ? relativePath : "";
    }
}
