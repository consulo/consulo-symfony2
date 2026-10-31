/**
 * Symfony data sources and Doctrine navigation in the database tool window, loaded only with the database plugin
 */
module fr.adrienbrault.idea.symfony2plugin.database {
    requires consulo.application.api;
    requires consulo.language.api;
    requires consulo.localize.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.virtual.file.system.api;
    requires consulo.database.datasource.api;
    requires consulo.database.datasource.jdbc.api;
    requires consulo.php.api;
    requires consulo.disposer.api;
    requires fr.adrienbrault.idea.symfony2plugin;

    exports fr.adrienbrault.idea.symfony2plugin.integrations.database;
}
