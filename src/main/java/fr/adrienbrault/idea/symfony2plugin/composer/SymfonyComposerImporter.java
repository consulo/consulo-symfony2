package fr.adrienbrault.idea.symfony2plugin.composer;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.php.composer.importer.ComposerImporter;
import consulo.php.composer.importer.ComposerPackage;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyMutableModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;

/**
 * Enables the Symfony module extension when a Composer project requiring Symfony is imported
 */
@ExtensionImpl
public class SymfonyComposerImporter implements ComposerImporter {
    @Override
    public boolean isApplicable(ComposerPackage composerPackage) {
        return composerPackage.requires("symfony/framework-bundle")
            || composerPackage.requires("symfony/symfony")
            || composerPackage.requiresAnyWithPrefix("symfony/");
    }

    @RequiredReadAction
    @Override
    public void process(Module module, ModifiableRootModel model, ComposerPackage composerPackage, VirtualFile packageRoot) {
        SymfonyMutableModuleExtension extension = model.getExtensionWithoutCheck(SymfonyMutableModuleExtension.class);
        if (extension == null) {
            return;
        }
        extension.setEnabled(true);
        IdeHelper.autoConfigure(module.getProject(), extension.getSettings(), model);
    }
}
