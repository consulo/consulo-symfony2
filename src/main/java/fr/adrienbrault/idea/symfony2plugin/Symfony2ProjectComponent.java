package fr.adrienbrault.idea.symfony2plugin;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent {
    /**
     * Registered through the top-level subclass {@link Symfony2ProjectComponentPostStartupActivity}
     * (nested classes cannot carry @ExtensionImpl)
     */
    public static class PostStartupActivity implements consulo.project.startup.PostStartupActivity, DumbAware {
        @Override
        public void runActivity(Project project, UIAccess uiAccess) {
            if (isEnabled(project)) {
                SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project);
            }

            if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
                checkProject(project);
            }
        }
    }

    final private static Logger LOG = Logger.getInstance("Symfony-Plugin");
    public static Logger getLogger() {
        return LOG;
    }

    public static Collection<VirtualFile> getContainerFiles(Project project) {
        Collection<ContainerFile> containerFiles = new ArrayList<>();

        List<ContainerFile> settingsContainerFiles = Settings.getInstance(project).containerFiles;
        if (settingsContainerFiles != null) {
            containerFiles.addAll(settingsContainerFiles.stream()
                .filter(containerFile -> containerFile.getPath() != null)
                .toList());
        }

        if (containerFiles.isEmpty()) {
            for (String s : ServiceContainerUtil.getContainerFiles(project)) {
                containerFiles.add(new ContainerFile(s));
            }
        }

        Collection<VirtualFile> validFiles = new ArrayList<>();
        for (ContainerFile containerFile : containerFiles) {
            VirtualFile virtualFile = containerFile.getVirtualFile(project);
            if (virtualFile != null && virtualFile.exists()) {
                validFiles.add(virtualFile);
            }
        }

        return validFiles;
    }

    private static void checkProject(Project project) {
        if(!isEnabled(project)
            && !SymfonyProjectState.getInstance(project).dismissEnableNotification
            && VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "vendor", "symfony") != null
            ) {

            IdeHelper.notifyEnableMessage(project);
        }
    }

    /**
     * True if any module of the project has the Symfony module extension enabled
     */
    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Settings.findProjectExtension(project) != null;
    }

    /**
     * If plugin is not enabled on first project start/indexing we will never get a filled
     * index until a forced cache rebuild, we check also for vendor path
     */
    public static boolean isEnabledForIndex(Project project) {

        if(isEnabled(project)) {
            return true;
        }

        if(VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "vendor", "symfony") != null) {
            return true;
        }

        // drupal8; this should not really here
        if(VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "core", "vendor", "symfony") != null) {
            return true;
        }

        return false;
    }

    /**
     * True if the module of the element has the Symfony module extension enabled; elements without a module
     * (libraries, light files) use {@link #isEnabled(Project)}
     */
    @RequiredReadAction
    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        if (psiElement == null) {
            return false;
        }

        Project project = psiElement.getProject();
        if (!isEnabled(project)) {
            return false;
        }

        Module module = Settings.findModule(psiElement);
        if (module == null) {
            return true;
        }

        return module.getExtension(SymfonyModuleExtension.class) != null;
    }
}
