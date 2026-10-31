package fr.adrienbrault.idea.symfony2plugin.module.extension;

import consulo.annotation.access.RequiredReadAction;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import org.jdom.Element;

/**
 * Symfony support of a module. The {@link Settings} bean is serialized into the extension element, skipping default values.
 */
public class SymfonyModuleExtension extends ModuleExtensionBase<SymfonyModuleExtension> {
    protected volatile Settings mySettings = new Settings();

    public SymfonyModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    /**
     * Read-only for the immutable extension; use {@link Settings#modify(consulo.module.Module, java.util.function.Consumer)} to change it
     */
    public Settings getSettings() {
        return mySettings;
    }

    @RequiredReadAction
    @Override
    public void commit(SymfonyModuleExtension mutableModuleExtension) {
        super.commit(mutableModuleExtension);

        mySettings = mutableModuleExtension.getSettings().copy();
    }

    @Override
    protected void getStateImpl(Element element) {
        mySettings.serializeInto(element);
    }

    @RequiredReadAction
    @Override
    protected void loadStateImpl(Element element) {
        mySettings = Settings.deserialize(element);
    }
}
