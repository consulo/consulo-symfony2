package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import org.jspecify.annotations.Nullable;

/**
 * Internal data class holding parsed Doctrine class metadata (className, repositoryClass, tableName).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record DoctrineClassMetadata(
    String className,
    @Nullable String repositoryClass,
    @Nullable String tableName
) {
}
