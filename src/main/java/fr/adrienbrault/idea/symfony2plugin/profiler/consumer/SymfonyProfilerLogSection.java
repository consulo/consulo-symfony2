package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

/** Deprecations are intentionally first; remaining levels follow Monolog severity. */
public enum SymfonyProfilerLogSection {
    DEPRECATION("Deprecations", true),
    EMERGENCY("Emergency", true),
    ALERT("Alert", false),
    CRITICAL("Critical", false),
    ERROR("Errors", true),
    WARNING("Warnings", true),
    SILENCED("Silenced", false),
    NOTICE("Notice", false),
    INFO("Info", false),
    DEBUG("Debug", false);

    private final String myTitle;
    private final boolean myImportant;

    SymfonyProfilerLogSection(String title, boolean important) {
        myTitle = title;
        myImportant = important;
    }

    public String getTitle() {
        return myTitle;
    }

    public boolean getImportant() {
        return myImportant;
    }
}
