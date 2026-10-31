package fr.adrienbrault.idea.symfony2plugin.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jspecify.annotations.Nullable;

/**
 * Enables Symfony support for a PHP module
 */
@ExtensionImpl
public class SymfonyModuleExtensionProvider implements ModuleExtensionProvider<SymfonyModuleExtension> {
    public static final String ID = "symfony";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return "php";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony");
    }

    @Override
    public Image getIcon() {
        return Symfony2Icons.SYMFONY;
    }

    @Override
    public ModuleExtension<SymfonyModuleExtension> createImmutableExtension(ModuleRootLayer layer) {
        return new SymfonyModuleExtension(getId(), layer);
    }

    @Override
    public MutableModuleExtension<SymfonyModuleExtension> createMutableExtension(ModuleRootLayer layer) {
        return new SymfonyMutableModuleExtension(getId(), layer);
    }
}
