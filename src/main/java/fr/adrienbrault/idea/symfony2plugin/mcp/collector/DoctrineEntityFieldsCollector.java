package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public final class DoctrineEntityFieldsCollector {
    private final Project myProject;

    public DoctrineEntityFieldsCollector(Project project) {
        myProject = project;
    }

    public String collect(String className) {
        String normalizedClassName = StringUtils.stripStart(className, "\\");

        PhpClass phpClass = PhpElementsUtil.getClassInterface(myProject, normalizedClassName);
        if (phpClass == null) {
            throw new McpToolException("Entity '" + className + "' not found. Make sure the class exists.");
        }

        Collection<DoctrineModelField> fields = EntityHelper.getModelFields(phpClass);

        if (fields.isEmpty()) {
            throw new McpToolException("Entity '" + className + "' has no fields or is not a valid Doctrine entity with metadata.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("name,column,type,relation,relationType,enumType,propertyType").append('\n');
        for (DoctrineModelField field : fields) {
            sb.append(
                McpCsvUtil.INSTANCE.escape(field.getName()) + "," +
                    McpCsvUtil.INSTANCE.escape(orEmpty(field.getColumn())) + "," +
                    McpCsvUtil.INSTANCE.escape(orEmpty(field.getTypeName())) + "," +
                    McpCsvUtil.INSTANCE.escape(orEmpty(field.getRelation())) + "," +
                    McpCsvUtil.INSTANCE.escape(orEmpty(field.getRelationType())) + "," +
                    McpCsvUtil.INSTANCE.escape(orEmpty(field.getEnumType())) + "," +
                    McpCsvUtil.INSTANCE.escape(String.join("|", field.getPropertyTypes()))
            ).append('\n');
        }

        return sb.toString();
    }

    private static String orEmpty(@Nullable String value) {
        return value != null ? value : "";
    }
}
