package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;

import java.util.Collection;

public final class DoctrineEntityCollector {
    private final Project myProject;

    public DoctrineEntityCollector(Project project) {
        myProject = project;
    }

    public String collect() {
        StringBuilder sb = new StringBuilder();
        Collection<DoctrineModel> entities = EntityHelper.getModelClasses(myProject);
        sb.append("className,filePath").append('\n');

        for (DoctrineModel entity : entities) {
            PhpClass phpClass = entity.getPhpClass();
            String filePath = "";
            PsiFile containingFile = phpClass.getContainingFile();
            VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
            if (virtualFile != null) {
                filePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
            }

            sb.append(McpCsvUtil.INSTANCE.escape(phpClass.getFQN())).append(",").append(McpCsvUtil.INSTANCE.escape(filePath)).append('\n');
        }

        return sb.toString();
    }
}
