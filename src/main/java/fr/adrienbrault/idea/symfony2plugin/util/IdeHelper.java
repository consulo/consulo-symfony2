package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.util.dataholder.Key;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiNavigationSupport;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.SymfonyProjectState;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyMutableModuleExtension;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IdeHelper {

    private static final String DIRECTORY_EXCLUDE_MESSAGE = "Directory '%s' marked as excluded for indexing";

    public static void openUrl(String url) {
        try {
            Platform.current().openInBrowser(url);
        }
        catch (RuntimeException ignored) {
        }
    }

    @Nullable
    public static VirtualFile createFile(Project project, FileType fileType, @Nullable VirtualFile root, String fileNameWithPath) {
        return createFile(project, fileType, root, fileNameWithPath, null);
    }

    @Nullable
    public static VirtualFile createFile(Project project, FileType fileType, @Nullable VirtualFile root, String fileNameWithPath, @Nullable String content) {

        if(root == null) {
            return null;
        }

        String[] filenameSplit = fileNameWithPath.split("/");
        String pathString = StringUtils.join(Arrays.copyOf(filenameSplit, filenameSplit.length - 1), "/");

        VirtualFile twigDirectory = VirtualFileUtil.findRelativeFile(root, filenameSplit);
        if(twigDirectory != null) {
            return null;
        }

        VirtualFile targetDir;
        try {
            targetDir = VirtualFileUtil.createDirectoryIfMissing(root, pathString);
        } catch (IOException ignored) {
            return null;
        }

        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        final PsiFile file = factory.createFileFromText(filenameSplit[filenameSplit.length - 1], fileType, content != null ? content : "");
        CodeStyleManager.getInstance(project).reformat(file);
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(targetDir);
        if(directory == null) {
            return null;
        }

        PsiElement add = directory.add(file);
        if(add instanceof PsiFile) {
            return ((PsiFile) add).getVirtualFile();
        }

        return null;
    }

    public static RunnableCreateAndOpenFile getRunnableCreateAndOpenFile(Project project, FileType fileType, VirtualFile rootVirtualFile, String fileName) {
        return new RunnableCreateAndOpenFile(project, fileType, rootVirtualFile, fileName);
    }

    public static class RunnableCreateAndOpenFile implements Runnable {

        private final FileType fileType;
        private final VirtualFile rootVirtualFile;
        private final String fileName;
        private final Project project;
        private String content;

        public RunnableCreateAndOpenFile(Project project, FileType fileType, VirtualFile rootVirtualFile, String fileName) {
            this.project = project;
            this.fileType = fileType;
            this.rootVirtualFile = rootVirtualFile;
            this.fileName = fileName;
        }

        public RunnableCreateAndOpenFile setContent(@Nullable String content) {
            this.content = content;
            return this;
        }

        @Override
        public void run() {
            VirtualFile virtualFile = createFile(project, fileType, rootVirtualFile, fileName, this.content);
            if(virtualFile != null) {
                OpenFileDescriptorFactory.getInstance(project).newBuilder(virtualFile).offset(0).build().navigate(true);
            }
        }
    }

    private static final Key<Notification> ENABLE_NOTIFICATION = Key.create("SymfonyEnableNotification");

    public static void notifyEnableMessage(final Project project) {
        Notification notification = NotificationService.getInstance()
            .newInfo(SymfonyNotificationGroupContributor.GROUP)
            .content(LocalizeValue.localizeTODO("Detected a Symfony project structure"))
            .create();

        notification.addAction(new NotificationAction(LocalizeValue.localizeTODO("Enable Symfony")) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e, Notification notification) {
                Project project1 = e.getData(Project.KEY);
                if (project1 == null) {
                    return;
                }

                enablePluginAndConfigure(project1);

                notification.expire();
            }
        });

        notification.addAction(new NotificationAction(LocalizeValue.localizeTODO("Don't show again")) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e, Notification notification) {
                // user doesnt want to show notification again
                SymfonyProjectState.getInstance(project).dismissEnableNotification = true;

                notification.expire();
            }
        });

        notification.setTitle(createNotificationTitle(project));
        notification.setIcon(Symfony2Icons.SYMFONY);

        project.putUserData(ENABLE_NOTIFICATION, notification);
        notification.notify(project);
    }

    /**
     * Expires the "Enable Symfony" notification once the Symfony module extension is enabled - like by the Composer
     * import, which runs after the project start check
     */
    public static void expireEnableMessage(Project project) {
        Notification notification = project.getUserData(ENABLE_NOTIFICATION);
        if (notification != null) {
            project.putUserData(ENABLE_NOTIFICATION, null);
            notification.expire();
        }
    }

    private static String createNotificationTitle(final Project project) {
        return "Symfony Support";
    }

    /**
     * Enables the Symfony module extension on the PHP modules of the project (or, without any PHP module, on the module
     * containing the project directory) and applies the auto-detected configuration to it.
     */
    @RequiredUIAccess
    public static Collection<String> enablePluginAndConfigure(Project project) {
        Collection<String> messages = new ArrayList<>();

        for (Module module : getSymfonyTargetModules(project)) {
            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();

            // Symfony is a child of the "php" extension
            MutableModuleExtension<?> phpExtension = model.getExtensionWithoutCheck("php");
            if (phpExtension != null && !phpExtension.isEnabled()) {
                phpExtension.setEnabled(true);
            }

            SymfonyMutableModuleExtension extension = model.getExtensionWithoutCheck(SymfonyMutableModuleExtension.class);
            if (extension == null) {
                model.dispose();
                continue;
            }

            extension.setEnabled(true);

            for (String message : autoConfigure(project, extension.getSettings(), model)) {
                if (!messages.contains(message)) {
                    messages.add(message);
                }
            }

            if (model.isChanged()) {
                WriteAction.run(model::commit);
            }
            else {
                model.dispose();
            }
        }

        return messages;
    }

    /**
     * Modules with the "php" module extension; if there are none, the module whose content contains the project directory
     */
    public static Collection<Module> getSymfonyTargetModules(Project project) {
        Collection<Module> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (module.getExtension("php") != null) {
                modules.add(module);
            }
        }

        if (modules.isEmpty()) {
            VirtualFile projectDir = ProjectUtil.getProjectDir(project);
            Module module = projectDir != null ? ModuleUtilCore.findModuleForFile(projectDir, project) : null;
            if (module != null) {
                modules.add(module);
            }
        }

        return modules;
    }

    /**
     * Detects the Symfony project structure and writes it into the given settings. If the layer is modifiable and belongs to the
     * module containing the project directory, generated directories (var/, public/build, public/bundles) are excluded on it;
     * the caller is responsible for committing the layer.
     */
    public static Collection<String> autoConfigure(Project project, Settings settings, @Nullable ModuleRootLayer layer) {
        Collection<String> messages = new ArrayList<>();

        /* Remove version info; prevent index issues
        Set<String> versions = SymfonyUtil.getVersions(project);
        if (!versions.isEmpty()) {
            messages.add("Symfony Version: " + versions.iterator().next());
        }
        */

        VirtualFile projectDir = ProjectUtil.getProjectDir(project);

        // Symfony 3.0 structure
        if (VirtualFileUtil.findRelativeFile(projectDir, "var", "cache") != null) {
            settings.pathToTranslation = "var/cache/dev/translations";
            messages.add("Translations: var/cache/dev/translations");
        }

        // Symfony 4.0 structure
        if (VirtualFileUtil.findRelativeFile(projectDir, "public") != null) {
            settings.directoryToWeb = "public";
            messages.add("Web Directory: public");
        }

        // There no clean version when "FooBar:Foo:foo.html.twig" was dropped or deprecated
        // So we disable it in the 4 branch by default; following with a default switch to "false" soon
        /* Remove version info; prevent index issues
        if (SymfonyUtil.isVersionGreaterThenEquals(project, "4.0")) {
            settings.twigBundleNamespaceSupport = false;
            messages.add("Twig: Bundle names disabled");
        }
        */

        // mark "var" as excluded directory to prevent indexing
        if (projectDir == null || !(layer instanceof ModifiableModuleRootLayer modifiableLayer)) {
            return messages;
        }

        Module moduleForFile = ModuleUtilCore.findModuleForFile(projectDir, project);
        if (moduleForFile == null || !moduleForFile.equals(layer.getModule())) {
            return messages;
        }

        for (ContentEntry contentEntry : modifiableLayer.getContentEntries()) {
            VirtualFile var = VirtualFileUtil.findRelativeFile(projectDir, "var");

            // second check for valid exclude
            if (var != null && (VirtualFileUtil.findRelativeFile(projectDir, "var", "cache") != null || VirtualFileUtil.findRelativeFile(projectDir, "var", "log") != null) && !hasAlreadyAnExclude(contentEntry, projectDir, "var")) {
                contentEntry.addFolder(var, ExcludedContentFolderTypeProvider.getInstance());

                if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "var"))) {
                    messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "var"));
                }
            }

            // encore build path + "var" are Symfony structure root
            VirtualFile publicBuild = VirtualFileUtil.findRelativeFile(projectDir, "public", "build");
            if (publicBuild != null && var != null && !hasAlreadyAnExclude(contentEntry, projectDir, "public/build")) {
                contentEntry.addFolder(publicBuild, ExcludedContentFolderTypeProvider.getInstance());

                if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/build"))) {
                    messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/build"));
                }
            }

            // assetic bundle + "var" are Symfony structure root
            VirtualFile publicBundles = VirtualFileUtil.findRelativeFile(projectDir, "public", "bundles");
            if (publicBundles != null && var != null && !hasAlreadyAnExclude(contentEntry, projectDir, "public/bundles")) {
                contentEntry.addFolder(publicBundles, ExcludedContentFolderTypeProvider.getInstance());

                if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/bundles"))) {
                    messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/bundles"));
                }
            }
        }

        return messages;
    }

    private static boolean hasAlreadyAnExclude(ContentEntry contentEntry, VirtualFile projectDir, String path) {
        return Arrays.stream(contentEntry.getFolders(LanguageContentFolderScopes.excluded())).anyMatch(excludeFolder -> {
            VirtualFile file = excludeFolder.getFile();
            if (file == null) {
                return false;
            }

            return path.equals(VirtualFileUtil.getRelativePath(file, projectDir, '/'));
        });
    }

    public static void navigateToPsiElement(PsiElement psiElement) {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(psiElement);
        if (descriptor != null) {
            descriptor.navigate(true);
        }
    }

    public static void showErrorHintIfAvailable(Editor editor, String text) {
        // consulo-port: no intention preview
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            return;
        }

        Runnable runnable = () -> HintManager.getInstance().showErrorHint(editor, text);

        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(runnable);
        } else {
            runnable.run();
        }
    }
}
