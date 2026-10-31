package fr.adrienbrault.idea.symfony2plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Project scoped UI state of the Symfony plugin which is not configuration (dismissed notifications, last used choices).
 * The configuration itself lives in the Symfony module extension, see {@link Settings}.
 * <p>
 * Keeps the storage of the former project settings component, so dismissed notifications survive the migration.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(
    name = "Symfony2PluginSettings",
    storages = {
        @Storage("symfony2.xml")
    }
)
public class SymfonyProjectState implements PersistentStateComponent<SymfonyProjectState> {
    public boolean dismissEnableNotification = false;
    public boolean dismissYamlSchemaNotification = false;

    /**
     * Last user selected output of service generator eg: yaml, xml, fluent, or phparray
     */
    public @Nullable String lastServiceGeneratorLanguage = null;

    public static SymfonyProjectState getInstance(Project project) {
        return project.getInstance(SymfonyProjectState.class);
    }

    @Override
    public SymfonyProjectState getState() {
        return this;
    }

    @Override
    public void loadState(SymfonyProjectState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
