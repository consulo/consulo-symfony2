package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SymfonyCommandCollector {
    private final Project myProject;

    public SymfonyCommandCollector(Project project) {
        myProject = project;
    }

    public String collect(@Nullable String fileGlob) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,className,filePath,options,arguments").append('\n');

        for (CommandRow command : collectCommandRows(fileGlob)) {
            sb.append(
                List.of(
                    command.name(),
                    command.className(),
                    command.filePath(),
                    serializeOptions(command),
                    serializeArguments(command)
                ).stream().map(it -> McpCsvUtil.INSTANCE.escape(it)).collect(Collectors.joining(","))
            ).append('\n');
        }

        return sb.toString();
    }

    private List<CommandRow> collectCommandRows(@Nullable String fileGlob) {
        String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;

        List<CommandRow> rows = new ArrayList<>();
        for (SymfonyCommand command : SymfonyCommandUtil.getCommands(myProject)) {
            PhpNamedElement target = SymfonyCommandUtil.resolveCommandTarget(myProject, command);

            String filePath = "";
            PsiFile containingFile = target != null ? target.getContainingFile() : null;
            VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
            if (virtualFile != null) {
                filePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
            }

            rows.add(new CommandRow(
                command.getName(),
                command.getFqn(),
                filePath,
                SymfonyCommandUtil.getCommandOptions(myProject, command),
                SymfonyCommandUtil.getCommandArguments(myProject, command)
            ));
        }

        return rows.stream()
            .filter(command -> normalizedFileGlob == null || McpGlobMatcher.INSTANCE.matches(command.filePath(), normalizedFileGlob))
            .sorted(Comparator.comparing(CommandRow::name))
            .collect(Collectors.toList());
    }

    private String serializeOptions(CommandRow command) {
        List<JsonObject> options = command.options().values()
            .stream()
            .sorted(Comparator.comparing(SymfonyCommandUtil.CommandOption::name))
            .map(option -> {
                JsonObject jsonObject = new JsonObject();
                addOptionalProperty(jsonObject, "name", option.name());
                addOptionalProperty(jsonObject, "shortcut", option.shortcut());
                addOptionalProperty(jsonObject, "description", option.description());
                addOptionalProperty(jsonObject, "defaultValue", option.defaultValue());
                return jsonObject;
            })
            .collect(Collectors.toList());

        if (options.isEmpty()) {
            return "";
        }

        JsonArray jsonArray = new JsonArray();
        options.forEach(jsonArray::add);
        return jsonArray.toString();
    }

    private String serializeArguments(CommandRow command) {
        List<JsonObject> arguments = command.arguments().values()
            .stream()
            .sorted(Comparator.comparing(SymfonyCommandUtil.CommandArgument::name))
            .map(argument -> {
                JsonObject jsonObject = new JsonObject();
                addOptionalProperty(jsonObject, "name", argument.name());
                addOptionalProperty(jsonObject, "description", argument.description());
                addOptionalProperty(jsonObject, "defaultValue", argument.defaultValue());
                return jsonObject;
            })
            .collect(Collectors.toList());

        if (arguments.isEmpty()) {
            return "";
        }

        JsonArray jsonArray = new JsonArray();
        arguments.forEach(jsonArray::add);
        return jsonArray.toString();
    }

    private static void addOptionalProperty(JsonObject jsonObject, String name, @Nullable String value) {
        if (value != null) {
            jsonObject.addProperty(name, value);
        }
    }

    private record CommandRow(
        String name,
        String className,
        String filePath,
        Map<String, SymfonyCommandUtil.CommandOption> options,
        Map<String, SymfonyCommandUtil.CommandArgument> arguments
    ) {
    }
}
