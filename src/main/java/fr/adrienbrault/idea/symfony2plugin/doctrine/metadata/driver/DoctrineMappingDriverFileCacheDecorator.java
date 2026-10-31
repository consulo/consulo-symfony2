package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.util.dataholder.Key;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-file cache decorator for {@link DoctrineMappingDriverInterface} implementations
 * that parse metadata from a single PSI file (XML/YAML mapping drivers).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMappingDriverFileCacheDecorator implements DoctrineMappingDriverInterface {

    private static final Key<CachedValue<Map<String, DoctrineMetadataModel>>> XML_CACHE = new Key<>("DOCTRINE_MAPPING_DRIVER_FILE_CACHE_XML");
    private static final Key<CachedValue<Map<String, DoctrineMetadataModel>>> YAML_CACHE = new Key<>("DOCTRINE_MAPPING_DRIVER_FILE_CACHE_YAML");

    private final DoctrineMappingDriverInterface delegate;

    private final Key<CachedValue<Map<String, DoctrineMetadataModel>>> cacheKey;

    public DoctrineMappingDriverFileCacheDecorator(DoctrineMappingDriverInterface delegate) {
        this.delegate = delegate;
        this.cacheKey = delegate instanceof DoctrineXmlMappingDriver ? XML_CACHE : YAML_CACHE;
    }

    @Nullable
    @Override
    public DoctrineMetadataModel getMetadata(DoctrineMappingDriverArguments arguments) {
        PsiFile file = arguments.getPsiFile();

        Map<String, DoctrineMetadataModel> cache = LanguageCachedValueUtil.getCachedValue(file, cacheKey, () ->
            CachedValueProvider.Result.create(
                new HashMap<>(),
                file,
                FileIndexCaches.getModificationTrackerForIndexId(file.getProject(), DoctrineMetadataFileStubIndex.KEY)
            )
        );

        String className = arguments.getClassName();
        if (!cache.containsKey(className)) {
            cache.put(className, delegate.getMetadata(arguments));
        }

        return cache.get(className);
    }
}
