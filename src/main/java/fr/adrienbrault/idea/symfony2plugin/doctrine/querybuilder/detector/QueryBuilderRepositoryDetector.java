package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector;

import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface QueryBuilderRepositoryDetector {

    @Nullable
    String getRepository(QueryBuilderRepositoryDetectorParameter parameter);

}
