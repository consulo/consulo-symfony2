package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.mcp.service.ServiceDefinitionGenerator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects MCP output for generated Symfony service definitions.
 *
 * This collector accepts one or more class names, delegates the actual definition generation
 * to {@link ServiceDefinitionGenerator}, and then enriches the generated YAML/XML/PHP output with
 * additional comment lines for constructor arguments that have multiple matching service ids.
 *
 * The extra comments are intended for MCP consumers only, so the tool can return both the
 * generated definition and relevant service-id alternatives in a single response.
 */
public final class ServiceDefinitionCollector {
    private final Project myProject;
    private final ServiceDefinitionGenerator myServiceDefinitionGenerator = new ServiceDefinitionGenerator();
    private final int myMaxServicesPerParameterSuggestion = 15;

    private record CommentSyntax(String lineFormat) {
    }

    public ServiceDefinitionCollector(Project project) {
        myProject = project;
    }

    public String collect(
        String classNames,
        ServiceBuilder.OutputType outputType,
        boolean useClassNameAsId
    ) {
        List<String> classes = new ArrayList<>();
        for (String className : classNames.split(",", -1)) {
            String trimmed = className.trim();
            if (!trimmed.isEmpty()) {
                classes.add(trimmed);
            }
        }

        if (classes.isEmpty()) {
            throw new McpToolException("className parameter is required.");
        }

        List<String> results = new ArrayList<>();
        for (String className : classes) {
            ServiceDefinitionGenerator.ServiceDefinitionModel model = myServiceDefinitionGenerator.createModel(myProject, className);
            if (model == null) {
                results.add("Error: Class not found: " + className);
                continue;
            }

            String definition = myServiceDefinitionGenerator.generate(myProject, className, outputType, useClassNameAsId);
            if (definition == null) {
                results.add("Error: Class not found: " + className);
                continue;
            }

            results.add(appendServiceSuggestions(definition, model.getModelParameters(), getCommentSyntax(outputType)));
        }

        return String.join("\n---\n", results);
    }

    private CommentSyntax getCommentSyntax(ServiceBuilder.OutputType outputType) {
        return switch (outputType) {
            case Yaml -> new CommentSyntax("# %s");
            case XML -> new CommentSyntax("<!-- %s -->");
            case Fluent, PhpArray -> new CommentSyntax("// %s");
        };
    }

    private String appendServiceSuggestions(
        String content,
        List<MethodParameter.MethodModelParameter> modelParameters,
        CommentSyntax commentSyntax
    ) {
        List<String> commentLines = formatServiceSuggestions(modelParameters, commentSyntax);
        if (commentLines.isEmpty()) {
            return content;
        }

        return content + "\n\n" + String.join("\n", commentLines);
    }

    private List<String> formatServiceSuggestions(
        List<MethodParameter.MethodModelParameter> modelParameters,
        CommentSyntax commentSyntax
    ) {
        if (modelParameters.isEmpty() || modelParameters.stream().noneMatch(it -> it.isPossibleService() && it.getPossibleServices().size() > 1)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        result.add(String.format(commentSyntax.lineFormat(), "Possible services per parameter:"));
        for (MethodParameter.MethodModelParameter modelParameter : modelParameters) {
            result.add(String.format(commentSyntax.lineFormat(), formatServiceSuggestion(modelParameter)));
        }
        return result;
    }

    private String formatServiceSuggestion(MethodParameter.MethodModelParameter modelParameter) {
        String parameterName = "$" + modelParameter.getParameter().getName();
        String declaredType = StringUtils.stripStart(modelParameter.getParameter().getDeclaredType().toString(), "\\");
        String displayType = StringUtils.isNotBlank(declaredType) ? declaredType : "mixed";
        String possibleServices = modelParameter.getPossibleServices()
            .stream()
            .limit(myMaxServicesPerParameterSuggestion)
            .map(service -> service.contains(",") ? "\"" + service + "\"" : service)
            .collect(Collectors.joining(", "));

        if (modelParameter.isPossibleService() && modelParameter.getPossibleServices().size() > 1) {
            return parameterName + " [" + displayType + "] => " + possibleServices;
        }
        else {
            return parameterName + " [" + displayType + "]";
        }
    }
}
