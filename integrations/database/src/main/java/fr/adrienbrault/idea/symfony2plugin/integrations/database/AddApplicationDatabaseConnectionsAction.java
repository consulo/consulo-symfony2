package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.database.datasource.DataSourceManager;
import consulo.database.datasource.configurable.EditablePropertiesHolder;
import consulo.database.datasource.configurable.GenericPropertyKeys;
import consulo.database.datasource.configurable.SecureString;
import consulo.database.datasource.model.EditableDataSource;
import consulo.database.datasource.model.EditableDataSourceModel;
import consulo.database.datasource.provider.DataSourceProvider;
import consulo.database.datasource.ui.DataSourceActionGroups;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogService;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Adds the data sources of the database connections found in the Symfony configuration
 */
@ActionImpl(id = "Symfony.AddApplicationDatabaseConnections", parents = @ActionParentRef(@ActionRef(id = DataSourceActionGroups.ADD_DATA_SOURCE)))
public class AddApplicationDatabaseConnectionsAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public AddApplicationDatabaseConnectionsAction() {
        super(
            LocalizeValue.localizeTODO("Add Symfony Application Database..."),
            LocalizeValue.localizeTODO("Add database connections from Symfony configuration (.env)"),
            Symfony2Icons.SYMFONY
        );
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabledAndVisible(project != null && Symfony2ProjectComponent.isEnabled(project));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ReadAction.nonBlocking(() -> new DotEnvConnectionProvider().getConnectionConfigs(project))
            .finishOnUiThread(app -> ModalityState.nonModal(), connections -> showConnections(project, connections))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredUIAccess
    private static void showConnections(Project project, List<DatabaseConnectionConfig> connections) {
        if (connections.isEmpty()) {
            Alerts.okInfo(LocalizeValue.localizeTODO("No database connections found in Symfony configuration.")).showAsync();
            return;
        }

        DialogService dialogService = project.getApplication().getInstance(DialogService.class);
        dialogService.build(project, new AddFoundDatabaseConnectionDialogDescriptor(connections, selected -> createDataSources(project, selected)))
            .showAsync();
    }

    @RequiredUIAccess
    private static void createDataSources(Project project, List<DatabaseConnectionConfig> connections) {
        Application application = project.getApplication();
        EditableDataSourceModel model = DataSourceManager.getInstance(project).createEditableModel();

        for (DatabaseConnectionConfig config : connections) {
            DataSourceProvider provider = findProvider(application, config.driver());
            if (provider == null) {
                Alerts.okError(LocalizeValue.localizeTODO("No data source provider '" + config.driver() + "' for connection '" + config.name() + "'."))
                    .showAsync();
                continue;
            }

            EditableDataSource dataSource = model.newDataSource("Symfony: " + config.name(), provider);
            EditablePropertiesHolder properties = dataSource.getProperties();
            if (config.host() != null) {
                properties.set(GenericPropertyKeys.HOST, config.host());
            }
            if (config.port() != null && config.port() > 0) {
                properties.set(GenericPropertyKeys.PORT, config.port());
            }
            if (config.username() != null) {
                properties.set(GenericPropertyKeys.LOGIN, config.username());
            }
            if (config.password() != null) {
                properties.set(GenericPropertyKeys.PASSWORD, SecureString.raw(config.password()));
            }
            if (config.database() != null) {
                properties.set(GenericPropertyKeys.DATABASE_NAME, config.database());
            }
        }

        model.commit();
    }

    @Nullable
    private static DataSourceProvider findProvider(Application application, String driver) {
        return application.getExtensionPoint(DataSourceProvider.class).findFirstSafe(provider -> provider.getId().equals(driver));
    }
}
