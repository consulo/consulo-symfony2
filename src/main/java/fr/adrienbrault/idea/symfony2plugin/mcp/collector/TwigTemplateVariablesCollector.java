package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.ForLoopVariableResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Collects all Twig variables available in a template with their PHP types and
 * first-level Twig-accessible properties (get/is/has shortcut methods + public fields).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TwigTemplateVariablesCollector {
    private final Project myProject;

    public TwigTemplateVariablesCollector(Project project) {
        myProject = project;
    }

    public String collect(@Nullable String templateInput, @Nullable String fileGlob) {
        PsiManager psiManager = PsiManager.getInstance(myProject);
        Map<String, PsiFile> resolvedTemplates = new LinkedHashMap<>();
        String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;

        if (templateInput != null) {
            for (String rawTemplateName : templateInput.split(",", -1)) {
                String templateName = rawTemplateName.trim();
                if (templateName.isBlank()) {
                    continue;
                }

                for (VirtualFile virtualFile : TwigUtil.getTemplateFiles(myProject, templateName)) {
                    PsiFile psiFile = psiManager.findFile(virtualFile);
                    if (psiFile != null) {
                        resolvedTemplates.putIfAbsent(templateName, psiFile);
                        break;
                    }
                }

                VirtualFile projectDir = ProjectUtil.getProjectDir(myProject);
                VirtualFile virtualFile = projectDir != null ? projectDir.findFileByRelativePath(templateName) : null;
                if (virtualFile != null) {
                    PsiFile psiFile = psiManager.findFile(virtualFile);
                    if (psiFile != null) {
                        for (String logicalTemplateName : TwigUtil.getTemplateNamesForFile(myProject, virtualFile)) {
                            resolvedTemplates.putIfAbsent(logicalTemplateName, psiFile);
                        }
                    }
                }
            }
        }

        if (normalizedFileGlob != null) {
            for (Map.Entry<String, Set<VirtualFile>> entry : new TreeMap<>(TwigUtil.getTemplateMap(myProject)).entrySet()) {
                String templateName = entry.getKey();

                VirtualFile matchingFile = null;
                for (VirtualFile virtualFile : entry.getValue()) {
                    String relativePath = getDisplayPath(virtualFile);
                    if (!relativePath.isBlank() && McpGlobMatcher.INSTANCE.matches(relativePath, normalizedFileGlob)) {
                        matchingFile = virtualFile;
                        break;
                    }
                }

                if (matchingFile == null) {
                    continue;
                }

                PsiFile psiFile = psiManager.findFile(matchingFile);
                if (psiFile != null) {
                    resolvedTemplates.putIfAbsent(templateName, psiFile);
                }
            }
        }

        if (resolvedTemplates.isEmpty()) {
            return emptyCsv();
        }

        if (resolvedTemplates.size() == 1) {
            return renderCsv(resolvedTemplates.values().iterator().next());
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, PsiFile> entry : resolvedTemplates.entrySet()) {
            String templateName = entry.getKey();
            PsiFile psiFile = entry.getValue();

            VirtualFile virtualFile = psiFile.getVirtualFile();
            String filePath = virtualFile != null ? getDisplayPath(virtualFile) : "";
            sb.append("template: ").append(templateName);
            if (!filePath.isBlank()) {
                sb.append(" => (file: ").append(filePath).append(")");
            }
            sb.append('\n');
            sb.append(renderCsv(psiFile));
            if (index < resolvedTemplates.size() - 1) {
                sb.append('\n');
            }
            index++;
        }

        return sb.toString();
    }

    private String renderCsv(PsiFile psiFile) {
        Map<String, PsiVariable> variables = TwigTypeResolveUtil.collectScopeVariables(psiFile);
        StringBuilder csv = new StringBuilder(emptyCsv());

        for (Map.Entry<String, PsiVariable> entry : new TreeMap<>(variables).entrySet()) {
            String varName = entry.getKey();
            PsiVariable psiVariable = entry.getValue();

            String typeStr = String.join("|", psiVariable.getTypes());
            String propertiesStr = String.join(",", collectPropertiesForTypes(psiVariable.getTypes()));

            csv.append(McpCsvUtil.INSTANCE.escape(varName)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(typeStr)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(propertiesStr)).append("\n");
        }

        return csv.toString();
    }

    private String emptyCsv() {
        return "variable,type,properties\n";
    }

    private String getDisplayPath(VirtualFile virtualFile) {
        String relativePath = VfsExUtil.getRelativeProjectPathStrict(myProject, virtualFile);
        if (relativePath == null) {
            relativePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
        }
        return relativePath.replace('\\', '/');
    }

    /**
     * Collects the first-level Twig-accessible property names from all PHP types.
     *
     * Delegates to the same method-name normalization as PhpTwigMethodLookupElement.getLookupString so the output
     * exactly matches what IDE completion shows for `{{ variable.X }}`:
     * - get/is/has shortcut methods → shortcut name (e.g. getName → name)
     * - other public non-set, non-magic methods → method name as-is
     * - public non-static fields → field name
     * - synthetic \loop variables → ForLoopVariableResolver.getLoopVariables(project)
     *
     * Types ending with `[]` have the suffix stripped to resolve the base class.
     */
    public List<String> collectPropertiesForTypes(Set<String> types) {
        Set<String> result = new TreeSet<>();

        if (types.contains(ForLoopVariableResolver.LOOP_VARIABLE_TYPE)) {
            result.addAll(ForLoopVariableResolver.getLoopVariables(myProject));
        }

        for (String type : types) {
            String baseType = StringUtils.stripEnd(type, "[]");
            if (PhpType.isPrimitiveType(baseType)) {
                continue;
            }
            PhpClass phpClass = PhpElementsUtil.getClassInterface(myProject, baseType);
            if (phpClass == null) {
                continue;
            }
            collectFromClass(phpClass, result);
        }

        return new ArrayList<>(result);
    }

    private void collectFromClass(PhpClass phpClass, Set<String> result) {
        // Use central Twig method accessibility check
        for (Method method : phpClass.getMethods()) {
            if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) {
                continue;
            }
            result.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method.getName()));
        }

        for (Field field : phpClass.getFields()) {
            if (field.getModifier().isPublic() && !field.getModifier().isStatic()) {
                result.add(field.getName());
            }
        }
    }
}
