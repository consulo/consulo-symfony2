package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class SymfonyRouteCollector {
    private final Project myProject;

    public SymfonyRouteCollector(Project project) {
        myProject = project;
    }

    public String collect(
        @Nullable String routeName,
        @Nullable String controller,
        @Nullable String urlPath,
        @Nullable String fileGlob
    ) {
        StringBuilder sb = new StringBuilder();
        Map<String, Route> allRoutes = RouteHelper.getAllRoutes(myProject);
        PhpIndex phpIndex = PhpIndex.getInstance(myProject);
        String normalizedUrlPath = urlPath != null && !urlPath.trim().isBlank()
            ? RouteHelper.normalizeRouteSearchPath(urlPath.trim())
            : null;
        Set<String> matchingRouteNames;
        if (normalizedUrlPath != null) {
            matchingRouteNames = new LinkedHashSet<>();
            for (Route route : RouteHelper.getRoutesForNormalizedPathWithPlaceholderMatch(myProject, normalizedUrlPath)) {
                matchingRouteNames.add(route.getName());
            }
        }
        else {
            matchingRouteNames = Collections.emptySet();
        }

        Map<String, Route> filteredRoutes = new LinkedHashMap<>();
        for (Map.Entry<String, Route> entry : allRoutes.entrySet()) {
            String name = entry.getKey();
            Route route = entry.getValue();

            boolean matchesName = routeName == null || StringUtils.containsIgnoreCase(name, routeName);
            boolean matchesController = controller == null || (route.getController() != null && StringUtils.containsIgnoreCase(route.getController(), controller));
            boolean matchesUrl = normalizedUrlPath == null || matchesUrl(route, normalizedUrlPath, matchingRouteNames);
            if (matchesName && matchesController && matchesUrl) {
                filteredRoutes.put(name, route);
            }
        }

        Set<String> controllerFqns = new LinkedHashSet<>();
        for (Route route : filteredRoutes.values()) {
            String routeController = route.getController();
            if (routeController != null && routeController.contains("::")) {
                controllerFqns.add(routeController);
            }
        }

        Map<String, Set<String>> templatesByController = TwigUtil.findTemplatesByControllers(myProject, controllerFqns);

        sb.append("name,controller,path,filePath,lineNumber,templates").append('\n');

        for (Route route : filteredRoutes.values()) {
            String routeController = route.getController();

            String controllerClass = null;
            Method controllerMethod = null;
            if (routeController != null && routeController.contains("::")) {
                String className = StringUtils.substringBefore(routeController, "::");
                String methodName = StringUtils.substringAfter(routeController, "::");
                controllerClass = className;
                controllerMethod = getControllerMethod(className, methodName);
            }
            else if (routeController != null) {
                controllerClass = routeController;
            }

            String filePath = null;
            if (controllerClass != null) {
                String fqn = controllerClass.startsWith("\\") ? controllerClass : "\\" + controllerClass;
                PhpClass phpClass = phpIndex.getClassesByFQN(fqn).stream().findFirst().orElse(null);
                PsiFile containingFile = phpClass != null ? phpClass.getContainingFile() : null;
                VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
                if (virtualFile != null) {
                    filePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
                }
            }
            if (filePath == null) {
                filePath = "";
            }

            String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;
            if (normalizedFileGlob != null && !McpGlobMatcher.INSTANCE.matches(filePath, normalizedFileGlob)) {
                continue;
            }

            String lineNumber = controllerMethod != null ? String.valueOf(getLineNumber(controllerMethod)) : "";

            String normalizedController = routeController != null
                ? StringUtils.stripStart(routeController.replace("::", "."), "\\")
                : null;

            String templates = "";
            if (normalizedController != null) {
                Set<String> controllerTemplates = templatesByController.get(normalizedController);
                if (controllerTemplates != null) {
                    templates = String.join(";", controllerTemplates);
                }
            }

            sb.append(McpCsvUtil.INSTANCE.escape(route.getName())).append(",")
                .append(McpCsvUtil.INSTANCE.escape(routeController != null ? routeController : "")).append(",")
                .append(McpCsvUtil.INSTANCE.escape(route.getPath() != null ? route.getPath() : "")).append(",")
                .append(McpCsvUtil.INSTANCE.escape(filePath)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(lineNumber)).append(",")
                .append(McpCsvUtil.INSTANCE.escape(templates)).append('\n');
        }

        return sb.toString();
    }

    private @Nullable Method getControllerMethod(String className, String methodName) {
        String fqn = className.startsWith("\\") ? className : "\\" + className;

        for (PhpClass phpClass : PhpIndex.getInstance(myProject).getClassesByFQN(fqn)) {
            Method method = phpClass.findMethodByName(methodName);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    private int getLineNumber(Method method) {
        PsiFile file = method.getContainingFile();
        if (file == null) {
            return 1;
        }

        Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
        if (document == null) {
            return 1;
        }

        return document.getLineNumber(method.getTextRange().getStartOffset()) + 1;
    }

    private static boolean matchesUrl(Route route, String urlPath, Set<String> matchingRouteNames) {
        String routePath = route.getPath();
        if (routePath == null) {
            return false;
        }

        return StringUtils.containsIgnoreCase(routePath, urlPath)
            || StringUtils.containsIgnoreCase(urlPath, routePath)
            || matchingRouteNames.contains(route.getName());
    }
}
