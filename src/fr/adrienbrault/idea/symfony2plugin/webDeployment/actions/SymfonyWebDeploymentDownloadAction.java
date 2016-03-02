package fr.adrienbrault.idea.symfony2plugin.webDeployment.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys;
import com.jetbrains.plugins.webDeployment.config.PublishConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;

public class SymfonyWebDeploymentDownloadAction extends DumbAwareAction {

    public void update(AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null) {
            e.getPresentation().setVisible(false);
            return;
        }

        WebServerConfig server = WebDeploymentDataKeys.SERVER_CONFIG.getData(e.getDataContext());
        if(server == null || !PublishConfig.getInstance(project).isDefault(server)) {
            e.getPresentation().setVisible(false);
            return;
        }

        e.getPresentation().setVisible(RemoteWebServerUtil.hasConfiguredRemoteFile(project));
    }

    public void actionPerformed(AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null) {
            return;
        }

        WebServerConfig server = WebDeploymentDataKeys.SERVER_CONFIG.getData(e.getDataContext());
        if(server == null || !PublishConfig.getInstance(project).isDefault(server)) {
            return;
        }

        new Task.Backgroundable(project, "Symfony: Downloading Files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    public void run() {
                        RemoteWebServerUtil.collectRemoteFiles(project);
                    }
                });
            }
        }.queue();
    }
}