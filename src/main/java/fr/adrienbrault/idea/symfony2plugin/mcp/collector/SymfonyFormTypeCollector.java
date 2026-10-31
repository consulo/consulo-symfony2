package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;

import java.util.Map;

public final class SymfonyFormTypeCollector {
    private final Project myProject;

    public SymfonyFormTypeCollector(Project project) {
        myProject = project;
    }

    public String collect() {
        StringBuilder sb = new StringBuilder();
        FormUtil.FormTypeCollector collector = new FormUtil.FormTypeCollector(myProject).collect();
        Map<String, FormTypeClass> formTypes = collector.getFormTypesMap();
        sb.append("name,className,filePath").append('\n');

        for (Map.Entry<String, FormTypeClass> entry : formTypes.entrySet()) {
            String name = entry.getKey();
            FormTypeClass formTypeClass = entry.getValue();

            PhpClass phpClass = formTypeClass.getPhpClass(myProject);
            String filePath = "";
            PsiFile containingFile = phpClass != null ? phpClass.getContainingFile() : null;
            VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
            if (virtualFile != null) {
                filePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
            }

            sb.append(McpCsvUtil.INSTANCE.escape(name)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(formTypeClass.getPhpClassName())).append(",")
                .append(McpCsvUtil.INSTANCE.escape(filePath)).append('\n');
        }

        return sb.toString();
    }
}
