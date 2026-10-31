package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

public final class ProfilerRendererException extends RuntimeException {
    private final String myCollectorName;

    public ProfilerRendererException(String collectorName, Exception cause) {
        super(cause.getMessage(), cause);
        myCollectorName = collectorName;
    }

    public String getCollectorName() {
        return myCollectorName;
    }
}
