package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Collects Twig template usages across a project: which PHP controllers render a template,
 * and which other Twig templates include/extend/embed/import/use/form_theme it.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TwigTemplateUsageCollector {
    private final Project myProject;

    public TwigTemplateUsageCollector(Project project) {
        myProject = project;
    }

    /**
     * Collects usages of all templates matching the given input (case-insensitive).
     *
     * Two resolution strategies run in parallel and results are merged:
     * 1. Partial template-name match — all index keys that contain the input string.
     * 2. File-path resolution — the input is tried as a path relative to the project root;
     *    if a file is found, its logical template names (via TwigUtil.getTemplateNamesForFile)
     *    are added as exact matches.  This handles inputs like "templates/home/index.html.twig"
     *    whose template name is just "home/index.html.twig".
     * 3. Optional file-glob filtering — resolved template files can be constrained by
     *    project-relative Ant-style glob patterns.
     *
     * Returns a CSV string with columns: template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path
     */
    public String collect(@Nullable String templateFilter, @Nullable String fileGlob) {
        FileBasedIndex index = FileBasedIndex.getInstance();
        GlobalSearchScope scope = GlobalSearchScope.allScope(myProject);
        String filterLower = templateFilter != null ? templateFilter.toLowerCase(Locale.ROOT) : null;
        String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;
        Set<String> globMatchedTemplatesFromPathInput = new LinkedHashSet<>();

        // Collect all matching template names using the internal template map
        Set<String> matchingTemplates = new TreeSet<>();

        // Strategy 1: partial template-name match using internal template map
        for (String templateName : TwigUtil.getTemplateMap(myProject).keySet()) {
            if (filterLower == null || templateName.toLowerCase(Locale.ROOT).contains(filterLower)) {
                matchingTemplates.add(templateName);
            }
        }

        // Strategy 2: resolve input as a file path relative to the project root and add its
        // logical template names (e.g. "templates/home/index.html.twig" → "home/index.html.twig")
        if (templateFilter != null) {
            VirtualFile projectDir = ProjectUtil.getProjectDir(myProject);
            VirtualFile virtualFile = projectDir != null ? projectDir.findFileByRelativePath(templateFilter) : null;
            if (virtualFile != null) {
                Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(myProject, virtualFile);
                matchingTemplates.addAll(templateNames);

                if (normalizedFileGlob != null) {
                    String relativePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
                    if (!relativePath.isBlank() && McpGlobMatcher.INSTANCE.matches(relativePath, normalizedFileGlob)) {
                        globMatchedTemplatesFromPathInput.addAll(templateNames);
                    }
                }
            }
        }

        if (normalizedFileGlob != null) {
            VirtualFile projectDir = ProjectUtil.getProjectDir(myProject);
            matchingTemplates.removeIf(templateName -> {
                if (globMatchedTemplatesFromPathInput.contains(templateName)) {
                    return false;
                }

                Set<VirtualFile> candidateFiles = new LinkedHashSet<>();
                Set<VirtualFile> templateFiles = TwigUtil.getTemplateMap(myProject).get(templateName);
                if (templateFiles != null) {
                    candidateFiles.addAll(templateFiles);
                }
                if (projectDir != null) {
                    VirtualFile file = projectDir.findFileByRelativePath(templateName);
                    if (file != null) {
                        candidateFiles.add(file);
                    }
                    file = projectDir.findFileByRelativePath("templates/" + templateName);
                    if (file != null) {
                        candidateFiles.add(file);
                    }
                }

                return candidateFiles.stream().noneMatch(virtualFile -> {
                    String relativePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
                    if (relativePath.isBlank()) {
                        return false;
                    }
                    return McpGlobMatcher.INSTANCE.matches(relativePath, normalizedFileGlob);
                });
            });
        }

        // Pre-scan {% use %} tags across all twig files for a reverse map:
        // usedTemplateName -> set of file paths that {% use %} it
        Map<String, Set<String>> useReverseMap = new LinkedHashMap<>();
        index.processValues(TwigBlockIndexExtension.KEY, "use", null, (file, templateSet) -> {
            String callerPath = VfsExUtil.getRelativeProjectPath(myProject, file);
            if (callerPath == null) {
                return true;
            }
            for (String usedTemplate : templateSet) {
                if (filterLower == null || usedTemplate.toLowerCase(Locale.ROOT).contains(filterLower)) {
                    matchingTemplates.add(usedTemplate);
                    useReverseMap.computeIfAbsent(usedTemplate, k -> new TreeSet<>()).add(callerPath);
                }
            }
            return true;
        }, scope);

        // Build reverse map: controller FQN ("App\Controller\Foo::bar") -> sorted list of (routeName, routePath)
        Map<String, List<String[]>> controllerToRoutes = new LinkedHashMap<>();
        for (Map.Entry<String, Route> entry : RouteHelper.getAllRoutes(myProject).entrySet()) {
            String routeName = entry.getKey();
            Route route = entry.getValue();
            String ctrl = route.getController();
            if (ctrl == null) {
                continue;
            }
            controllerToRoutes.computeIfAbsent(ctrl, k -> new ArrayList<>()).add(new String[]{routeName, route.getPath() != null ? route.getPath() : ""});
        }

        StringBuilder csv = new StringBuilder("template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path\n");

        for (String templateName : matchingTemplates) {
            // PHP controller usages
            Set<String> controllers = new TreeSet<>();
            for (TemplateUsage usage : index.getValues(PhpTwigTemplateUsageStubIndex.KEY, templateName, scope)) {
                for (String fqn : usage.getScopes()) {
                    // Convert "App\Controller\HomeController.index" -> "App\Controller\HomeController::index"
                    int lastDot = fqn.lastIndexOf('.');
                    controllers.add(lastDot >= 0 ? fqn.substring(0, lastDot) + "::" + fqn.substring(lastDot + 1) : fqn);
                }
            }

            // Twig include / embed / import / form_theme
            Set<String> twigIncludes = new TreeSet<>();
            Set<String> twigEmbeds = new TreeSet<>();
            Set<String> twigImports = new TreeSet<>();
            Set<String> twigFormThemes = new TreeSet<>();

            index.processValues(TwigIncludeStubIndex.KEY, templateName, null, (file, includeObj) -> {
                String callerPath = VfsExUtil.getRelativeProjectPath(myProject, file);
                if (callerPath == null) {
                    return true;
                }
                for (TemplateInclude.TYPE type : includeObj.getTypes()) {
                    switch (type) {
                        case INCLUDE, INCLUDE_FUNCTION, SOURCE_FUNCTION -> twigIncludes.add(callerPath);
                        case EMBED -> twigEmbeds.add(callerPath);
                        case IMPORT, FROM -> twigImports.add(callerPath);
                        case FORM_THEME -> twigFormThemes.add(callerPath);
                        default -> {
                        }
                    }
                }
                return true;
            }, scope);

            // Twig extends
            Set<String> twigExtends = new TreeSet<>();
            index.processValues(TwigExtendsStubIndex.KEY, templateName, null, (file, value) -> {
                String callerPath = VfsExUtil.getRelativeProjectPath(myProject, file);
                if (callerPath == null) {
                    return true;
                }
                twigExtends.add(callerPath);
                return true;
            }, scope);

            Set<String> twigUses = useReverseMap.getOrDefault(templateName, Collections.emptySet());

            // Twig component usages: template → file → component names → files using the component
            Set<String> twigComponents = new TreeSet<>();
            GlobalSearchScope componentScope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(myProject), TwigFileType.INSTANCE);
            for (VirtualFile virtualFile : TwigUtil.getTemplateFiles(myProject, templateName)) {
                PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
                if (!(psiFile instanceof TwigFile twigFile)) {
                    continue;
                }

                for (String componentName : UxUtil.getTemplateComponentNames(twigFile)) {
                    String normalized = TwigComponentUsageStubIndex.normalizeComponentName(componentName);
                    if (normalized == null) {
                        continue;
                    }
                    for (VirtualFile usageFile : index.getContainingFiles(TwigComponentUsageStubIndex.KEY, normalized, componentScope)) {
                        if (usageFile.equals(virtualFile)) {
                            continue;
                        }
                        String relativePath = VfsExUtil.getRelativeProjectPath(myProject, usageFile);
                        if (relativePath != null) {
                            twigComponents.add(relativePath);
                        }
                    }
                }
            }

            // Collect route names and paths for all controllers in this row (ordered by controller, then route name)
            List<String[]> routeEntries = new ArrayList<>();
            for (String ctrl : controllers) {
                List<String[]> routes = controllerToRoutes.get(ctrl);
                if (routes != null) {
                    routes.stream().sorted(Comparator.comparing((String[] it) -> it[0])).forEach(routeEntries::add);
                }
            }
            String routeNames = routeEntries.stream().map(it -> it[0]).collect(Collectors.joining(";"));
            String routePaths = routeEntries.stream().map(it -> it[1]).collect(Collectors.joining(";"));

            csv.append(escapeCsv(templateName)).append(",")
                .append(escapeCsv(String.join(";", controllers))).append(",")
                .append(escapeCsv(String.join(";", twigIncludes))).append(",")
                .append(escapeCsv(String.join(";", twigEmbeds))).append(",")
                .append(escapeCsv(String.join(";", twigExtends))).append(",")
                .append(escapeCsv(String.join(";", twigImports))).append(",")
                .append(escapeCsv(String.join(";", twigUses))).append(",")
                .append(escapeCsv(String.join(";", twigFormThemes))).append(",")
                .append(escapeCsv(String.join(";", twigComponents))).append(",")
                .append(escapeCsv(routeNames)).append(",")
                .append(escapeCsv(routePaths)).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        else {
            return value;
        }
    }
}
