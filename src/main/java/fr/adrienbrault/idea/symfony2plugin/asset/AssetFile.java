package fr.adrienbrault.idea.symfony2plugin.asset;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetFile {
    private final VirtualFile assetFile;

    private final AssetEnum.Position assetPosition;

    @Nullable
    private VirtualFile relativeFolder;

    private String prefix = "";

    private String string = null;

    public AssetFile(VirtualFile assetFile, AssetEnum.Position assetPosition, VirtualFile relativeFolder, String prefix) {
        this(assetFile, assetPosition, relativeFolder);
        this.prefix = prefix;
    }

    public AssetFile(VirtualFile assetFile, AssetEnum.Position assetPosition, VirtualFile relativeFolder) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
        this.relativeFolder = relativeFolder;
    }

    private AssetFile(VirtualFile assetFile, AssetEnum.Position assetPosition) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
    }

    public static AssetFile createVirtualManifestEntry(VirtualFile manifestJson, String name) {
        AssetFile myAssetFile = new AssetFile(manifestJson, AssetEnum.Position.Web);

        myAssetFile.string = name;

        return myAssetFile;
    }

    public VirtualFile getFile() {
        return assetFile;
    }

    public AssetEnum.Position getAssetPosition() {
        return assetPosition;
    }

    public String toString() {
        return this.string != null
            ? this.string
            : this.prefix + (relativeFolder != null ? VirtualFileUtil.getRelativePath(assetFile, relativeFolder, '/') : "");
    }
}
