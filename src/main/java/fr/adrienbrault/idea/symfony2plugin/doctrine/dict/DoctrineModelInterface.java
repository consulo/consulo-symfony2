package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineModelInterface {

    String getClassName();

    @Nullable
    String getRepositoryClass();

    @Nullable
    default String getTableName() {
        return null;
    }
}
