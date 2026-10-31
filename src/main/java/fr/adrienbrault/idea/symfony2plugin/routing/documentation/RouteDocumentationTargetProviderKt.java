package fr.adrienbrault.idea.symfony2plugin.routing.documentation;

import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Top-level declarations of the former RouteDocumentationTargetProvider.kt
 */
public final class RouteDocumentationTargetProviderKt {
    private RouteDocumentationTargetProviderKt() {
    }

    public static String renderRouteDocumentation(Collection<Route> routes, int twigTemplates) {
        Set<String> rendered = new LinkedHashSet<>();

        for (Route route : routes) {
            StringBuilder content = new StringBuilder();

            String path = RouteHelper.getRouteUrl(route);
            if (path != null) {
                content.append("<br>Path: <code>").append(escape(path)).append("</code>");
            }

            if (!route.getMethods().isEmpty()) {
                List<String> methods = new ArrayList<>();
                for (String method : route.getMethods()) {
                    methods.add(method.toUpperCase(Locale.ROOT));
                }
                methods.sort(null);
                content.append("<br>Methods: <code>").append(escape(String.join(", ", methods))).append("</code>");
            }

            List<String> defaults = new ArrayList<>();
            for (String key : route.getDefaults().keySet()) {
                if (!"_controller".equals(key)) {
                    defaults.add(key);
                }
            }
            defaults.sort(null);

            if (!defaults.isEmpty()) {
                content.append("<br>Defaults: ");
                List<String> items = new ArrayList<>();
                for (String it : defaults) {
                    items.add("<code>" + escape(it) + "</code>");
                }
                content.append(String.join(", ", items));
            }

            if (!route.getRequirements().isEmpty()) {
                content.append("<br>Requirements: ");
                List<String> items = new ArrayList<>();
                for (Map.Entry<String, String> entry : new TreeMap<>(route.getRequirements()).entrySet()) {
                    items.add("<code>" + escape(entry.getKey()) + ": " + escape(entry.getValue()) + "</code>");
                }
                content.append(String.join(", ", items));
            }

            String controller = route.getController();
            if (controller != null && !controller.isBlank()) {
                content.append("<br>Controller: <code>").append(escape(controller)).append("</code>");
            }

            if (twigTemplates > 0) {
                content.append("<br>Twig usages: ").append(twigTemplates);
            }

            content.append("<br><a href=\"").append(RouteDocumentationTargetKt.ROUTE_FIND_USAGES_LINK).append("\">Find usages</a>");

            String text = content.toString();
            if (text.startsWith("<br>")) {
                text = text.substring("<br>".length());
            }

            rendered.add(DocumentationMarkup.CONTENT_START + text + DocumentationMarkup.CONTENT_END);
        }

        List<String> sorted = new ArrayList<>(rendered);
        sorted.sort(null);

        return String.join("<hr>", sorted);
    }

    private static String escape(String value) {
        String text = value.length() > 1000 ? value.substring(0, 1000) + "…" : value;

        return StringUtil.escapeXmlEntities(text);
    }
}
