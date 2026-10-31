package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.database.datasource.jdbc.provider.JdbcDataSourceProvider;
import consulo.database.datasource.ui.DataSourceActionGroups;
import consulo.database.datasource.ui.DataSourceKeys;
import consulo.database.datasource.ui.DataSourceOpenableNode;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;

import java.util.Collection;

/**
 * Navigates from a table of the database tool window to the Doctrine entity mapped to it
 */
@ActionImpl(id = "Symfony.Navigation.FromTableToDoctrineEntity", parents = @ActionParentRef(@ActionRef(id = DataSourceActionGroups.TREE_POPUP)))
public class NavigateToDoctrineEntityFromDbTableAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public NavigateToDoctrineEntityFromDbTableAction() {
        super(
            LocalizeValue.localizeTODO("Go To Related Doctrine Entity"),
            LocalizeValue.localizeTODO("Navigate to corresponding Doctrine entity for this table"),
            Symfony2Icons.DOCTRINE
        );
    }

    @Override
    public void update(AnActionEvent e) {
        DataSourceOpenableNode node = e.getData(DataSourceKeys.OPENABLE_NODE);
        e.getPresentation().setEnabledAndVisible(
            e.getData(Project.KEY) != null && node != null && node.getDataSource().getProvider() instanceof JdbcDataSourceProvider
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        DataSourceOpenableNode node = e.getData(DataSourceKeys.OPENABLE_NODE);
        if (project == null || node == null) {
            return;
        }

        String tableName = getTableName(node.getChildId());
        ReadAction.nonBlocking(() -> DoctrineMetadataUtil.findEntityByTableName(project, tableName))
            .finishOnUiThread(app -> ModalityState.nonModal(), NavigateToDoctrineEntityFromDbTableAction::navigate)
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredUIAccess
    private static void navigate(Collection<PhpClass> classes) {
        if (!classes.isEmpty()) {
            classes.iterator().next().navigate(true);
        }
    }

    /**
     * The child id of a JDBC table is its name with the schema in front, like {@code public.users}
     */
    private static String getTableName(String childId) {
        int dot = childId.lastIndexOf('.');
        return dot < 0 ? childId : childId.substring(dot + 1);
    }
}
