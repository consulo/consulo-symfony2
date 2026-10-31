package fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterVisitor {
    private final String className;

    private final String method;

    private final int parameterIndex;

    public ParameterVisitor(String className, String method, int parameterIndex) {
        this.className = className;
        this.method = method;
        this.parameterIndex = parameterIndex;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }
}
