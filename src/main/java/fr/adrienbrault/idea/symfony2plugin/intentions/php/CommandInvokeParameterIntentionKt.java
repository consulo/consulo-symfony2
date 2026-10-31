package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * consulo-port: top-level declarations of upstream CommandInvokeParameterIntention.kt
 */
public final class CommandInvokeParameterIntentionKt {
    static final String AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";

    /**
     * Map of FQN (without leading backslash) to variable name
     */
    static final Map<String, String> AVAILABLE_COMMAND_PARAMETERS = new LinkedHashMap<>();

    static {
        AVAILABLE_COMMAND_PARAMETERS.put("Symfony\\Component\\Console\\Input\\InputInterface", "input");
        AVAILABLE_COMMAND_PARAMETERS.put("Symfony\\Component\\Console\\Output\\OutputInterface", "output");
        AVAILABLE_COMMAND_PARAMETERS.put("Symfony\\Component\\Console\\Cursor", "cursor");
        AVAILABLE_COMMAND_PARAMETERS.put("Symfony\\Component\\Console\\Style\\SymfonyStyle", "io");
        AVAILABLE_COMMAND_PARAMETERS.put("Symfony\\Component\\Console\\Application", "application");
    }

    private CommandInvokeParameterIntentionKt() {
    }

    /**
     * Formats a FQN for display as "ClassName (namespace)"
     * e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle" -> "SymfonyStyle (Symfony\\Component\\Console\\Style)"
     */
    static String formatCommandParameterDisplayName(String fqn) {
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
     * Filters out parameters whose types are already present in the method signature.
     *
     * @param method The method to check
     * @return List of available FQNs (e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle")
     */
    public static List<String> getAvailableCommandParameterFqns(Method method) {
        Set<String> existingParameterTypes = getExistingCommandParameterTypes(method);

        List<String> availableFqns = new ArrayList<>();
        for (String fqn : AVAILABLE_COMMAND_PARAMETERS.keySet()) {
            if (!existingParameterTypes.contains("\\" + fqn)) {
                availableFqns.add(fqn);
            }
        }

        return availableFqns;
    }

    private static Set<String> getExistingCommandParameterTypes(Method method) {
        Set<String> types = new LinkedHashSet<>();
        for (Parameter parameter : method.getParameters()) {
            types.addAll(parameter.getDeclaredType().getTypes());
        }
        return types;
    }
}
