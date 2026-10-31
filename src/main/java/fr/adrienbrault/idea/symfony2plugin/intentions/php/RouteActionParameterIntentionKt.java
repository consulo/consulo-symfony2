package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpAttributesOwner;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * consulo-port: top-level declarations of upstream RouteActionParameterIntention.kt
 */
public final class RouteActionParameterIntentionKt {
    /**
     * Map of FQN (without leading backslash) to variable name
     */
    static final Map<String, String> AVAILABLE_ROUTE_ACTION_PARAMETERS = new LinkedHashMap<>();

    static {
        AVAILABLE_ROUTE_ACTION_PARAMETERS.put("Symfony\\Component\\HttpFoundation\\Request", "request");
        AVAILABLE_ROUTE_ACTION_PARAMETERS.put("Symfony\\Component\\Security\\Core\\User\\UserInterface", "user");
    }

    private RouteActionParameterIntentionKt() {
    }

    /**
     * Formats a FQN for display as "ClassName (namespace)"
     */
    static String formatRouteActionParameterDisplayName(String fqn) {
        int lastBackslash = fqn.lastIndexOf('\\');
        if (lastBackslash == -1) {
            return fqn;
        }
        String className = fqn.substring(lastBackslash + 1);
        String namespace = fqn.substring(0, lastBackslash);
        return className + " (" + namespace + ")";
    }

    /**
     * Returns the list of FQNs (without leading backslash) that are available to be added to the given method.
     */
    public static List<String> getAvailableRouteActionParameterFqns(Method method) {
        Set<String> existingParameterTypes = getExistingRouteActionParameterTypes(method);

        List<String> availableFqns = new ArrayList<>();
        for (String fqn : AVAILABLE_ROUTE_ACTION_PARAMETERS.keySet()) {
            if (!existingParameterTypes.contains("\\" + fqn)) {
                availableFqns.add(fqn);
            }
        }

        return availableFqns;
    }

    private static Set<String> getExistingRouteActionParameterTypes(Method method) {
        Set<String> types = new LinkedHashSet<>();
        for (Parameter parameter : method.getParameters()) {
            types.addAll(parameter.getDeclaredType().getTypes());
        }
        return types;
    }

    /**
     * Checks if the method is a route action.
     * A method is a route action if:
     * - It has a Route attribute/annotation directly on the method, OR
     * - The method is __invoke and the class has a Route attribute/annotation
     */
    static boolean isMethodARouteAction(Method method) {
        // Check if method has Route attribute or annotation
        if (hasRouteAnnotationOrAttribute(method)) {
            return true;
        }

        // For __invoke methods, also check if class has Route attribute/annotation
        if ("__invoke".equals(method.getName())) {
            PhpClass phpClass = method.getContainingClass();
            if (phpClass != null && hasRouteAnnotationOrAttribute(phpClass)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasRouteAnnotationOrAttribute(PhpAttributesOwner method) {
        // Check for Route attributes
        for (String route : RouteHelper.ROUTE_ANNOTATIONS) {
            if (!method.getAttributes(route).isEmpty()) {
                return true;
            }
        }

        // Check for Route annotations in PHPDoc
        if (method instanceof PhpNamedElement phpNamedElement) {
            PhpDocComment docComment = phpNamedElement.getDocComment();
            if (docComment != null) {
                return AnnotationUtil.getPhpDocCommentAnnotationContainer(docComment).getFirstPhpDocBlock(RouteHelper.ROUTE_ANNOTATIONS) != null;
            }
        }

        return false;
    }
}
