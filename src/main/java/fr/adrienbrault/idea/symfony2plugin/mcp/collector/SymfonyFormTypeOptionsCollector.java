package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class SymfonyFormTypeOptionsCollector {
    private final Project myProject;

    public SymfonyFormTypeOptionsCollector(Project project) {
        myProject = project;
    }

    public String collect(String formType) {
        String normalizedFormType;
        if (formType.contains("\\")) {
            normalizedFormType = "\\" + StringUtils.stripStart(formType, "\\");
        }
        else {
            normalizedFormType = formType;
        }

        Map<String, FormOption> options = new LinkedHashMap<>();

        FormOptionsUtil.visitFormOptions(myProject, normalizedFormType, (psiElement, option, formClass, optionEnum) -> {
            if (!options.containsKey(option)) {
                options.put(option, new FormOption(option, formClass, optionEnum, formClass.getPhpClass()));
            }
            else {
                FormOption formOption = options.get(option);
                if (formOption != null) {
                    formOption.addOptionEnum(optionEnum);
                }
            }
        });

        if (options.isEmpty()) {
            throw new McpToolException("Form type '" + formType + "' not found or has no options.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("name,type,source").append('\n');
        for (Map.Entry<String, FormOption> entry : options.entrySet()) {
            String name = entry.getKey();
            FormOption formOption = entry.getValue();

            String types = formOption.getOptionEnum().stream().map(Enum::name).collect(Collectors.joining("|"));
            String source = formOption.getFormClass().getPhpClass().getFQN();

            sb.append(McpCsvUtil.INSTANCE.escape(name)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(types)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(source)).append('\n');
        }

        return sb.toString();
    }
}
