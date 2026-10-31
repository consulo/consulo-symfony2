package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TwigExtensionCollector {
    private final Project myProject;

    public TwigExtensionCollector(Project project) {
        myProject = project;
    }

    public String collect(
        @Nullable String search,
        boolean includeFilters,
        boolean includeFunctions,
        boolean includeTests,
        boolean includeTags
    ) {
        StringBuilder sb = new StringBuilder();
        PhpIndex phpIndex = PhpIndex.getInstance(myProject);
        sb.append("extension_type,name,className,methodName,parameters").append('\n');

        String searchLower = search != null ? search.toLowerCase(Locale.ROOT) : null;

        if (includeFilters) {
            Map<String, TwigExtension> filters = TwigExtensionParser.getFilters(myProject);
            appendExtensions(sb, "filter", filters, searchLower, phpIndex);
        }

        if (includeFunctions) {
            Map<String, TwigExtension> functions = TwigExtensionParser.getFunctions(myProject);
            appendExtensions(sb, "function", functions, searchLower, phpIndex);
        }

        if (includeTests) {
            Map<String, TwigExtension> tests = TwigExtensionParser.getSimpleTest(myProject);
            appendExtensions(sb, "test", tests, searchLower, phpIndex);
        }

        if (includeTags) {
            Set<String> tags = TwigUtil.getNamedTokenParserTags(myProject);
            for (String name : tags) {
                if (searchLower == null || name.toLowerCase(Locale.ROOT).contains(searchLower)) {
                    sb.append("tag,").append(McpCsvUtil.INSTANCE.escape(name)).append(",,,").append('\n');
                }
            }
        }

        return sb.toString();
    }

    private void appendExtensions(
        StringBuilder sb,
        String extensionType,
        Map<String, TwigExtension> extensions,
        @Nullable String searchLower,
        PhpIndex phpIndex
    ) {
        for (Map.Entry<String, TwigExtension> entry : extensions.entrySet()) {
            String name = entry.getKey();
            TwigExtension extension = entry.getValue();
            if (searchLower == null || name.toLowerCase(Locale.ROOT).contains(searchLower)) {
                ExtensionSignature signature = parseExtensionSignature(extension.getSignature(), phpIndex);
                String paramsStr = signature.parameters() != null ? String.join(",", signature.parameters()) : "";
                sb.append(extensionType).append(",")
                    .append(McpCsvUtil.INSTANCE.escape(name)).append(",")
                    .append(McpCsvUtil.INSTANCE.escape(signature.className() != null ? signature.className() : "")).append(",")
                    .append(McpCsvUtil.INSTANCE.escape(signature.methodName() != null ? signature.methodName() : "")).append(",")
                    .append(McpCsvUtil.INSTANCE.escape(paramsStr)).append('\n');
            }
        }
    }

    private ExtensionSignature parseExtensionSignature(@Nullable String signature, PhpIndex phpIndex) {
        if (signature == null) {
            return new ExtensionSignature(null, null, null);
        }

        String classAndMethod = StringUtils.removeStart(StringUtils.removeStart(signature, "#M#C"), "#M#M");
        String[] parts = classAndMethod.split("\\.", -1);

        String className = parts.length >= 2 ? parts[0] : null;
        String methodName = parts.length >= 2 ? parts[1] : null;

        List<String> parameters;
        try {
            Collection<? extends PhpNamedElement> phpNamedElements = phpIndex.getBySignature(signature);
            PhpNamedElement first = phpNamedElements.isEmpty() ? null : phpNamedElements.iterator().next();
            parameters = null;
            if (first instanceof Function function) {
                List<String> names = new ArrayList<>();
                for (Parameter parameter : function.getParameters()) {
                    if (!parameter.getName().startsWith("_")) {
                        names.add(parameter.getName());
                    }
                }
                if (!names.isEmpty()) {
                    parameters = names;
                }
            }
        }
        catch (Exception e) {
            parameters = null;
        }

        return new ExtensionSignature(className, methodName, parameters);
    }

    private record ExtensionSignature(
        @Nullable String className,
        @Nullable String methodName,
        @Nullable List<String> parameters
    ) {
    }
}
