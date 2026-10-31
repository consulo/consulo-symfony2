package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Collects Twig components for MCP usage.
 *
 * Returns component metadata and ready-to-use Twig syntax snippets for AI agents.
 */
public final class TwigComponentCollector {
    private final Project myProject;

    public TwigComponentCollector(Project project) {
        myProject = project;
    }

    public String collect(@Nullable String search) {
        String searchLower = search != null && !search.trim().isBlank() ? search.trim().toLowerCase(Locale.ROOT) : null;

        List<String> componentNames = UxUtil.getAllComponentNames(myProject)
            .stream()
            .map(UxUtil.TwigComponent::name)
            .filter(name -> !name.isBlank())
            .filter(name -> searchLower == null || name.toLowerCase(Locale.ROOT).contains(searchLower))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder(
            "component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks\n"
        );

        for (String componentName : componentNames) {
            ComponentRow row = buildRow(componentName);
            csv.append(escapeCsv(row.componentName())).append(',')
                .append(escapeCsv(row.templateRelativePath())).append(',')
                .append(escapeCsv(row.componentTag())).append(',')
                .append(escapeCsv(row.twigComponentSyntax())).append(',')
                .append(escapeCsv(row.componentPrintBlockSyntax())).append(',')
                .append(escapeCsv(row.twigTagCompositionSyntax())).append(',')
                .append(escapeCsv(row.props())).append(',')
                .append(escapeCsv(row.templateBlocks())).append('\n');
        }

        return csv.toString();
    }

    private ComponentRow buildRow(String componentName) {
        Set<String> templateRelativePaths = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<VirtualFile> templateVirtualFiles = new LinkedHashSet<>();

        for (PsiFile psiFile : UxUtil.getComponentTemplates(myProject, componentName)) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }

            templateVirtualFiles.add(virtualFile);
            String relativePath = VfsExUtil.getRelativeProjectPath(myProject, virtualFile);
            if (relativePath != null) {
                templateRelativePaths.add(relativePath);
            }
        }

        Set<String> blocks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Collection<String> blockNames : TwigUtil.getBlockNamesForFiles(myProject, templateVirtualFiles).values()) {
            for (String blockName : blockNames) {
                if (!blockName.isBlank()) {
                    blocks.add(blockName);
                }
            }
        }

        Set<String> props = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (PhpClass phpClass : UxUtil.getTwigComponentPhpClasses(myProject, componentName)) {
            UxUtil.visitComponentVariables(phpClass, pair -> {
                String name = pair.getFirst();
                if (name != null && !name.isBlank()) {
                    props.add(name);
                }
            });
        }

        UxUtil.visitComponentTemplateProps(myProject, componentName, pair -> {
            String name = pair.getFirst();
            if (name != null && !name.isBlank()) {
                props.add(name);
            }
        });

        String componentLiteral = escapeTwigSingleQuotedString(componentName);

        String printBlockSyntax = blocks.stream()
            .map(it -> "{{ block('" + escapeTwigSingleQuotedString(it) + "') }}")
            .collect(Collectors.joining(";"));

        String tagBlocks = blocks.stream()
            .map(it -> "{% block " + escapeTwigBlockName(it) + " %}{% endblock %}")
            .collect(Collectors.joining(""));

        String twigTagCompositionSyntax = "{% component '" + componentLiteral + "' %}" + tagBlocks + "{% endcomponent %}";

        return new ComponentRow(
            componentName,
            "<twig:" + componentName + "></twig:" + componentName + ">",
            String.join(";", templateRelativePaths),
            "{{ component('" + componentLiteral + "') }}",
            printBlockSyntax,
            twigTagCompositionSyntax,
            String.join(";", props),
            String.join(";", blocks)
        );
    }

    private String escapeCsv(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        else {
            return value;
        }
    }

    private String escapeTwigSingleQuotedString(String value) {
        return value.replace("'", "\\'");
    }

    private String escapeTwigBlockName(String value) {
        return value.replace("'", "");
    }

    private record ComponentRow(
        String componentName,
        String componentTag,
        String templateRelativePath,
        String twigComponentSyntax,
        String componentPrintBlockSyntax,
        String twigTagCompositionSyntax,
        String props,
        String templateBlocks
    ) {
    }
}
