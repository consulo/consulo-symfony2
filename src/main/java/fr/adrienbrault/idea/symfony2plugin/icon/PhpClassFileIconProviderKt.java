package fr.adrienbrault.idea.symfony2plugin.icon;

import consulo.ui.image.OverlayPosition;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

/**
 * Top-level functions of the former {@code PhpClassFileIconProvider.kt}.
 */
public final class PhpClassFileIconProviderKt {
    private PhpClassFileIconProviderKt() {
    }

    public static Image createDecoratedFileIcon(Image baseIcon, Image badgeIcon) {
        return ImageEffects.overlay(baseIcon, badgeIcon, OverlayPosition.BOTTOM_RIGHT);
    }
}
