package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommand {
    private final String name;

    private final String fqn;

    private final @Nullable String methodName;

    /**
     * @param fqn starting with "\"
     */
    public SymfonyCommand(String name, String fqn) {
        this(name, fqn, null);
    }

    /**
     * @param fqn starting with "\"
     */
    public SymfonyCommand(String name, String fqn, @Nullable String methodName) {
        if (!fqn.startsWith("\\")) {
            throw new IllegalArgumentException("Invalid fqn: " + fqn);
        }

        this.name = name;
        this.fqn = fqn;
        this.methodName = methodName;
    }
    
    public String getName() {
        return name;
    }

    public String getFqn() {
        return fqn;
    }

    public @Nullable String getMethodName() {
        return methodName;
    }

    public boolean isMethodCommand() {
        return methodName != null && !methodName.isBlank();
    }
}
