package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import jakarta.annotation.Nullable;

/**
 * A database connection found in the Symfony configuration
 *
 * @param driver   {@code mysql}, {@code postgresql} or {@code sqlite}
 * @param database the database name, or the file of a SQLite database
 */
public record DatabaseConnectionConfig(String name,
                                       String driver,
                                       @Nullable String host,
                                       @Nullable Integer port,
                                       @Nullable String database,
                                       @Nullable String username,
                                       @Nullable String password) {
}
