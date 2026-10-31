package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.CompiledTwigComponent;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Symfony UX TwigComponent runtime metadata from the compiled container.
 *
 * Symfony writes the final component configuration to the service
 * "ux.twig_component.component_factory":
 *
 * - top-level argument 4: component metadata keyed by component name
 * - top-level argument 5: class FQN to component name map
 */
public final class UxComponentFactoryParser extends AbstractServiceParser {

    private final Map<String, CompiledTwigComponent> myComponents = new LinkedHashMap<>();
    private final Map<String, String> myClassMap = new LinkedHashMap<>();

    public Map<String, CompiledTwigComponent> getComponents() {
        return myComponents;
    }

    public Map<String, String> getClassMap() {
        return myClassMap;
    }

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='ux.twig_component.component_factory']";
    }

    @Override
    public synchronized void parser(InputStream file, VirtualFile sourceFile, Project project) {
        NodeList services = parserer(file);
        if (services == null) {
            return;
        }

        for (int i = 0; i < services.getLength(); i++) {
            if (!(services.item(i) instanceof Element service)) {
                continue;
            }

            List<Element> arguments = directArgumentChildren(service);
            Element componentsArgument = arguments.size() > 4 ? arguments.get(4) : null;
            if (componentsArgument != null) {
                parseComponentsArgument(componentsArgument);
            }

            Element classMapArgument = arguments.size() > 5 ? arguments.get(5) : null;
            if (classMapArgument != null) {
                parseClassMapArgument(classMapArgument);
            }
        }
    }

    private void parseComponentsArgument(Element argument) {
        for (Element componentArgument : directArgumentChildren(argument)) {
            String outerName = componentArgument.getAttribute("key").strip();
            if (outerName.isBlank()) {
                outerName = null;
            }

            Map<String, String> values = keyedDirectArgumentValues(componentArgument);

            String name = values.get("key");
            if (name == null || name.isBlank()) {
                name = outerName;
            }

            if (name == null) {
                continue;
            }

            String phpClass = normalizePhpClass(values.get("class"));
            String template = nullIfBlank(values.get("template"));
            String templateFromMethod = nullIfBlank(values.get("template_from_method"));

            myComponents.put(name, new CompiledTwigComponent(name, phpClass, template, templateFromMethod));
        }
    }

    private void parseClassMapArgument(Element argument) {
        for (Element mapArgument : directArgumentChildren(argument)) {
            String phpClass = normalizePhpClass(mapArgument.getAttribute("key"));
            if (phpClass == null) {
                continue;
            }

            String componentName = mapArgument.getTextContent().strip();
            if (!componentName.isBlank()) {
                myClassMap.put(phpClass, componentName);
            }
        }
    }

    private Map<String, String> keyedDirectArgumentValues(Element argument) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Element child : directArgumentChildren(argument)) {
            String key = child.getAttribute("key").strip();
            if (!key.isBlank()) {
                values.put(key, child.getTextContent().strip());
            }
        }

        return values;
    }

    private List<Element> directArgumentChildren(Element element) {
        List<Element> arguments = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "argument".equals(child.getNodeName())) {
                arguments.add((Element) child);
            }
        }

        return arguments;
    }

    private @Nullable String normalizePhpClass(@Nullable String value) {
        String className = value != null ? nullIfBlank(value.strip()) : null;
        if (className == null) {
            return null;
        }

        return "\\" + StringUtils.stripStart(className, "\\");
    }

    private static @Nullable String nullIfBlank(@Nullable String value) {
        return value != null && !value.isBlank() ? value : null;
    }
}
