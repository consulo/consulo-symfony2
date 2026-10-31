package fr.adrienbrault.idea.symfony2plugin.icon;

import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

/**
 * Top-level functions of the former {@code PhpClassFileIconProvider.kt}.
 */
public final class PhpClassFileIconProviderKt {
    private PhpClassFileIconProviderKt() {
    }

    public static Image createDecoratedFileIcon(Image baseIcon, Image badgeIcon) {
        // TODO consulo-port: badge position (SwingConstants.SOUTH_EAST) not supported; ImageEffects.layered stacks layers at the origin
        return ImageEffects.layered(baseIcon, badgeIcon);
    }
}
