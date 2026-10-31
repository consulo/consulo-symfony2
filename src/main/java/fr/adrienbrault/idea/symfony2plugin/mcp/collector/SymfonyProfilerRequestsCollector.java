package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerTextRenderer;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SymfonyProfilerRequestsCollector {
    private final Project myProject;

    public SymfonyProfilerRequestsCollector(Project project) {
        myProject = project;
    }

    public String collect(
        @Nullable String url,
        @Nullable String hash,
        @Nullable String controller,
        @Nullable String route,
        int limit
    ) {
        ProfilerIndexInterface profilerIndex = ProfilerFactoryUtil.createIndex(myProject);
        if (profilerIndex == null) {
            throw new McpToolException("No profiler index available. Make sure the Symfony profiler is enabled and accessible.");
        }

        List<ProfilerRequestInterface> requests = profilerIndex.getRequests();

        if (requests.isEmpty()) {
            throw new McpToolException("No profiler requests found. Make sure the Symfony profiler is enabled and has recorded requests.");
        }

        return formatRequests(requests, url, hash, controller, route, limit);
    }

    public String formatRequests(
        Iterable<? extends ProfilerRequestInterface> requests,
        @Nullable String url,
        @Nullable String hash,
        @Nullable String controller,
        @Nullable String route,
        int limit
    ) {
        int effectiveLimit = limit > 0 ? limit : 25;
        List<ProfilerRequestInterface> filteredRequests = new ArrayList<>();
        for (ProfilerRequestInterface request : requests) {
            if (filteredRequests.size() >= effectiveLimit) {
                break;
            }

            DefaultDataCollectorInterface collectorData = request.getCollector(DefaultDataCollectorInterface.class);

            String controllerValue = collectorData != null && collectorData.getController() != null ? collectorData.getController() : "";
            String routeValue = collectorData != null && collectorData.getRoute() != null ? collectorData.getRoute() : "";

            if (url != null && !StringUtils.containsIgnoreCase(request.getUrl(), url)) {
                continue;
            }
            if (hash != null && !StringUtils.containsIgnoreCase(request.getHash(), hash)) {
                continue;
            }
            if (controller != null && !StringUtils.containsIgnoreCase(controllerValue, controller)) {
                continue;
            }
            if (route != null && !StringUtils.containsIgnoreCase(routeValue, route)) {
                continue;
            }

            filteredRequests.add(request);
        }

        Set<String> controllerFqns = new LinkedHashSet<>();
        for (ProfilerRequestInterface request : filteredRequests) {
            DefaultDataCollectorInterface collectorData = request.getCollector(DefaultDataCollectorInterface.class);
            String controllerFqn = collectorData != null ? collectorData.getController() : null;
            if (controllerFqn != null && StringUtils.isNotBlank(controllerFqn)) {
                controllerFqns.add(controllerFqn);
            }
        }

        Map<String, Set<String>> templatesByController = TwigUtil.findTemplatesByControllers(myProject, controllerFqns);

        StringBuilder sb = new StringBuilder();
        sb.append("hash,profiledAt,method,url,statusCode,profilerUrl,controller,route,entryView,renderTemplate,renderedTemplates,formTypes").append('\n');
        for (ProfilerRequestInterface request : filteredRequests) {
            DefaultDataCollectorInterface collectorData = request.getCollector(DefaultDataCollectorInterface.class);

            String controllerValue = collectorData != null && collectorData.getController() != null ? collectorData.getController() : "";
            String routeValue = collectorData != null && collectorData.getRoute() != null ? collectorData.getRoute() : "";
            String entryView = collectorData != null && collectorData.getTemplate() != null ? collectorData.getTemplate() : "";

            Set<String> controllerTemplates = templatesByController.get(normalizeControllerScope(controllerValue));
            String renderTemplate = controllerTemplates != null
                ? controllerTemplates.stream().sorted().collect(Collectors.joining(";"))
                : "";

            String renderedTemplates = collectorData != null && collectorData.getRenderedTemplates() != null
                ? collectorData.getRenderedTemplates().stream().limit(3).collect(Collectors.joining(";"))
                : "";

            String formTypes = collectorData != null && collectorData.getFormTypes() != null
                ? String.join("|", collectorData.getFormTypes())
                : "";

            Long time = request.getTime();
            String profiledAt = time != null ? ProfilerTextRenderer.INSTANCE.formatTimestamp(time) : null;

            sb.append(
                McpCsvUtil.INSTANCE.escape(request.getHash()) + "," +
                    McpCsvUtil.INSTANCE.escape(profiledAt != null ? profiledAt : "") + "," +
                    McpCsvUtil.INSTANCE.escape(request.getMethod() != null ? request.getMethod() : "") + "," +
                    McpCsvUtil.INSTANCE.escape(request.getUrl()) + "," +
                    request.getStatusCode() + "," +
                    McpCsvUtil.INSTANCE.escape(request.getProfilerUrl()) + "," +
                    McpCsvUtil.INSTANCE.escape(controllerValue) + "," +
                    McpCsvUtil.INSTANCE.escape(routeValue) + "," +
                    McpCsvUtil.INSTANCE.escape(entryView) + "," +
                    McpCsvUtil.INSTANCE.escape(renderTemplate) + "," +
                    McpCsvUtil.INSTANCE.escape(renderedTemplates) + "," +
                    McpCsvUtil.INSTANCE.escape(formTypes)
            ).append('\n');
        }

        return sb.toString();
    }

    private String normalizeControllerScope(String controller) {
        return StringUtils.stripStart(controller, "\\").replace("::", ".");
    }
}
