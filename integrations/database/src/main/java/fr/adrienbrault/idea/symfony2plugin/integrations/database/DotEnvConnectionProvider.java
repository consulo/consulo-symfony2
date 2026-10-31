package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import consulo.annotation.access.RequiredReadAction;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database connections from the {@code DATABASE_URL} variables of the .env files, and the SQLite databases in {@code var/}.
 * <p>
 * Supports Symfony's standard DATABASE_URL format:
 * <pre>
 *   mysql://user:pass@host:port/database
 *   postgresql://user:pass@host:port/database
 *   sqlite:///path/to/database.db
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DotEnvConnectionProvider {
    // driver://user:pass@host:port/database
    private static final Pattern DB_URL_PATTERN =
        Pattern.compile("^(mysql|mariadb|postgresql|pgsql|postgres|sqlite)://(?:([^:@]*):?([^@]*)@)?([^:/]+)(?::(\\d+))?/(.+)$");

    // sqlite:///path or sqlite:///:memory:
    private static final Pattern SQLITE_URL_PATTERN = Pattern.compile("^sqlite:///(.+)$");

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");

    @RequiredReadAction
    public List<DatabaseConnectionConfig> getConnectionConfigs(Project project) {
        List<DatabaseConnectionConfig> configs = new ArrayList<>();
        Map<String, String> envVars = DotEnvUtil.getEnvironmentVariablesWithValues(project);

        String databaseUrl = envVars.get("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            configs.addAll(parseUrl(databaseUrl, "default", project));
        }

        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            if (key.equals("DATABASE_URL")) {
                continue;
            }

            String connectionName = null;
            if (key.startsWith("DATABASE_URL_")) {
                connectionName = key.substring("DATABASE_URL_".length()).toLowerCase(Locale.ROOT);
            }
            else if (key.endsWith("_DATABASE_URL")) {
                connectionName = key.substring(0, key.length() - "_DATABASE_URL".length()).toLowerCase(Locale.ROOT);
            }

            String value = entry.getValue();
            if (connectionName != null && value != null && !value.isBlank()) {
                configs.addAll(parseUrl(value, connectionName, project));
            }
        }

        configs.addAll(scanSqliteDirectory(project));

        return uniqueByPath(configs);
    }

    private List<DatabaseConnectionConfig> parseUrl(String url, String name, Project project) {
        Matcher matcher = DB_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            String driver = normalizeDriver(matcher.group(1));
            String username = emptyToNull(matcher.group(2));
            String password = emptyToNull(matcher.group(3));
            String host = matcher.group(4);
            int port = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : getDefaultPort(driver);
            String database = matcher.group(6).replaceAll("\\?.*$", "");

            return List.of(new DatabaseConnectionConfig(name, driver, host, port, database, username, password));
        }

        Matcher sqliteMatcher = SQLITE_URL_PATTERN.matcher(url);
        if (sqliteMatcher.matches()) {
            return resolveSqlitePlaceholders(sqliteMatcher.group(1), name, project);
        }

        return List.of();
    }

    /**
     * Resolve Symfony placeholders in a SQLite path.
     * Replaces %kernel.project_dir% with the project root, then tries
     * %kernel.environment% with prod/dev/test/stage. Skips any result
     * that still contains unresolved %placeholder%.
     */
    private List<DatabaseConnectionConfig> resolveSqlitePlaceholders(String rawPath, String name, Project project) {
        VirtualFile projectDir = getProjectDir(project);

        String path = rawPath;
        if (projectDir != null) {
            path = path.replace("%kernel.project_dir%", projectDir.getPath());
        }

        if (!PLACEHOLDER_PATTERN.matcher(path).find()) {
            return List.of(new DatabaseConnectionConfig(name, "sqlite", null, null, path, null, null));
        }

        List<DatabaseConnectionConfig> results = new ArrayList<>();
        for (String env : new String[]{"prod", "dev", "test", "stage"}) {
            String resolved = path.replace("%kernel.environment%", env);
            if (!PLACEHOLDER_PATTERN.matcher(resolved).find()) {
                String connectionName = results.isEmpty() ? name : name + " (" + env + ")";
                results.add(new DatabaseConnectionConfig(connectionName, "sqlite", null, null, resolved, null, null));
            }
        }
        return results;
    }

    /**
     * Scan the project's var/ directory for any .db files.
     * This is independent of DATABASE_URL parsing — it discovers SQLite databases
     * that exist on disk regardless of how they are referenced in configuration.
     */
    private List<DatabaseConnectionConfig> scanSqliteDirectory(Project project) {
        VirtualFile projectDir = getProjectDir(project);
        if (projectDir == null) {
            return List.of();
        }

        VirtualFile varDir = projectDir.findChild("var");
        if (varDir == null || !varDir.isDirectory()) {
            return List.of();
        }

        List<DatabaseConnectionConfig> configs = new ArrayList<>();
        for (VirtualFile child : varDir.getChildren()) {
            if (!child.isDirectory() && child.getName().endsWith(".db") && child.isValid()) {
                String name = child.getName().substring(0, child.getName().length() - ".db".length());
                configs.add(new DatabaseConnectionConfig(name, "sqlite", null, null, child.getPath(), null, null));
            }
        }
        return configs;
    }

    @Nullable
    private static VirtualFile getProjectDir(Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            return baseDir;
        }
        String basePath = project.getBasePath();
        return basePath == null ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    private static List<DatabaseConnectionConfig> uniqueByPath(List<DatabaseConnectionConfig> configs) {
        Set<String> seen = new LinkedHashSet<>();
        return configs.stream().filter(config -> seen.add(Objects.toString(config.database(), ""))).toList();
    }

    private static String normalizeDriver(String driver) {
        return switch (driver.toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "mysql";
            case "postgresql", "pgsql", "postgres" -> "postgresql";
            case "sqlite" -> "sqlite";
            default -> driver;
        };
    }

    private static int getDefaultPort(String driver) {
        return switch (driver.toLowerCase(Locale.ROOT)) {
            case "mysql" -> 3306;
            case "postgresql" -> 5432;
            default -> 0;
        };
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
