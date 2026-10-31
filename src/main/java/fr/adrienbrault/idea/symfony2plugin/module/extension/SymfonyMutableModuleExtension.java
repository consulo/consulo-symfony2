package fr.adrienbrault.idea.symfony2plugin.module.extension;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TabbedLayout;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.SettingsForm;
import fr.adrienbrault.idea.symfony2plugin.profiler.ui.ProfilerSettingsDialog;
import fr.adrienbrault.idea.symfony2plugin.ui.ContainerSettingsForm;
import fr.adrienbrault.idea.symfony2plugin.ui.MethodParameterReferenceSettingsForm;
import fr.adrienbrault.idea.symfony2plugin.ui.MethodSignatureTypeSettingsForm;
import fr.adrienbrault.idea.symfony2plugin.ui.RoutingSettingsForm;
import fr.adrienbrault.idea.symfony2plugin.ui.TwigSettingsForm;
import org.jspecify.annotations.Nullable;

/**
 * Editable copy of {@link SymfonyModuleExtension}. The configuration panel writes straight into the copied
 * {@link Settings} bean, which is copied back into the immutable extension on commit.
 */
public class SymfonyMutableModuleExtension extends SymfonyModuleExtension implements MutableModuleExtension<SymfonyModuleExtension> {
    public SymfonyMutableModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createConfigurationComponent(Disposable uiDisposable, Runnable updateOnCheck) {
        Project project = getProject();
        Settings settings = getSettings();

        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(
            LocalizeValue.localizeTODO("General"),
            ScrollableLayout.create(new SettingsForm(project, getModuleRootLayer(), settings).createComponent(uiDisposable))
        );
        tabs.addTab(LocalizeValue.localizeTODO("Container"), new ContainerSettingsForm(project, settings).createComponent());
        tabs.addTab(LocalizeValue.localizeTODO("Routing"), new RoutingSettingsForm(project, settings).createComponent());
        tabs.addTab(LocalizeValue.localizeTODO("Twig / Template"), new TwigSettingsForm(project, settings).createComponent(uiDisposable));
        tabs.addTab(LocalizeValue.localizeTODO("Method References"), new MethodParameterReferenceSettingsForm(project, settings).createComponent());
        tabs.addTab(LocalizeValue.localizeTODO("Type Provider"), new MethodSignatureTypeSettingsForm(project, settings).createComponent());
        tabs.addTab(LocalizeValue.localizeTODO("Profiler"), new ProfilerSettingsDialog(project, settings).createComponent(uiDisposable));
        return tabs;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myIsEnabled = enabled;
    }

    @Override
    public boolean isModified(SymfonyModuleExtension originalExtension) {
        return myIsEnabled != originalExtension.isEnabled() || !getSettings().isSame(originalExtension.getSettings());
    }
}
