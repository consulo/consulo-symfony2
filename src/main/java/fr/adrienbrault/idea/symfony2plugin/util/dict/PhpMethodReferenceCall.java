package fr.adrienbrault.idea.symfony2plugin.util.dict;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMethodReferenceCall {

    private final String clazz;

    private final String[] methods;

    private final int index;

    public PhpMethodReferenceCall(String clazz, String... method) {
        this(clazz, method, 0);
    }

    public PhpMethodReferenceCall(String clazz, String method, int index) {
        this(clazz, new String[] { method }, index);
    }

    public PhpMethodReferenceCall(String clazz, int index, String... method) {
        this(clazz, method, index);
    }

    public PhpMethodReferenceCall(String clazz, String[] methods, int index) {
        this.clazz = clazz;
        this.methods = methods;
        this.index = index;
    }

    public String getClassName() {
        return this.clazz;
    }

    public String[] getMethods() {
        return this.methods;
    }

    public int getIndex() {
        return this.index;
    }
}
