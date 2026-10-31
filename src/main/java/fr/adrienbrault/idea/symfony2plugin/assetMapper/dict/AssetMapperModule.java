package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record AssetMapperModule(
    MappingFileEnum sourceType,
    VirtualFile sourceFile,
    String key,
    @Nullable String path,
    @Nullable String url,
    @Nullable String version,
    @Nullable Boolean entrypoint,
    @Nullable String type
) {
}
