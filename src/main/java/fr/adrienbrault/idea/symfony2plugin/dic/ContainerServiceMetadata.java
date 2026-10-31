package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Carries service metadata for one contributing definition so ContainerService can keep
 * multiple sources side by side; any merged view is computed by the consumer later.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record ContainerServiceMetadata(
    // Optional resource/prototype definition id that expanded this class, eg "App\\"
    @Nullable String resourceServiceId,
    boolean lazy,
    boolean abstractDefinition,
    boolean autowire,
    boolean autoconfigure,
    boolean deprecated,
    boolean publicService,
    @Nullable String alias,
    @Nullable String parent,
    @Nullable String decorates,
    @Nullable String decorationInnerName,
    Set<String> tags,
    // Raw resource glob patterns from the contributing resource/prototype definition
    Set<String> resource,
    // Raw exclude patterns from the contributing resource/prototype definition
    Set<String> exclude,
    SourceKind sourceKind
) {
    public ContainerServiceMetadata(
        @Nullable String resourceServiceId,
        boolean lazy,
        boolean abstractDefinition,
        boolean autowire,
        boolean autoconfigure,
        boolean deprecated,
        boolean publicService,
        @Nullable String alias,
        @Nullable String parent,
        @Nullable String decorates,
        @Nullable String decorationInnerName,
        Collection<String> tags,
        Collection<String> resource,
        Collection<String> exclude,
        SourceKind sourceKind
    ) {
        this(
            resourceServiceId,
            lazy,
            abstractDefinition,
            autowire,
            autoconfigure,
            deprecated,
            publicService,
            normalizeNullable(alias),
            normalizeNullable(parent),
            normalizeNullable(decorates),
            normalizeNullable(decorationInnerName),
            normalize(tags),
            normalize(resource),
            normalize(exclude),
            sourceKind
        );
    }

    private static Set<String> normalize(Collection<String> values) {
        return values.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    @Nullable
    private static String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static ContainerServiceMetadata empty(SourceKind sourceKind) {
        return new ContainerServiceMetadata(
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            null,
            null,
            null,
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            sourceKind
        );
    }

    public enum SourceKind {
        RESOURCE_PROTOTYPE,
        COMPILED_CONTAINER,
        INDEXED_SERVICE
    }
}
