package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TextComponentAccessor;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathPresentable;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UiSettingsUtil {

    /**
     * Consulo: the file chooser is asynchronous; the future completes with {@code null} when nothing was selected
     */
    @RequiredUIAccess
    public static CompletableFuture<@Nullable String> getPathDialog(Project project, FileType fileType) {
        return getPathDialog(project, fileType, null);
    }

    /**
     * Consulo: the file chooser is asynchronous; the future completes with {@code null} when nothing was selected
     */
    @RequiredUIAccess
    public static CompletableFuture<@Nullable String> getPathDialog(Project project, FileType fileType, @Nullable String current) {
        VirtualFile projectDirectory = ProjectUtil.getProjectDir(project);

        VirtualFile selectedFileBefore = null;
        if(current != null) {
            selectedFileBefore = VirtualFileUtil.findRelativeFile(current, projectDirectory);
        }

        return FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor(fileType),
                project,
                selectedFileBefore
        ).thenApply(selectedFile -> {
            if (null == selectedFile) {
                return null;
            }

            String path = VirtualFileUtil.getRelativePath(selectedFile, projectDirectory, '/');
            if (null == path) {
                path = selectedFile.getPath();
            }

            return path;
        });
    }

    public static UiFilePathPresentable getPresentableFilePath(Project project, UiFilePathInterface uiFilePath) {
        String info = uiFilePath.exists(project) ? "EXISTS" : "NOT FOUND";
        return new UiFilePathPresentable(uiFilePath.getPath(), info);
    }

    /**
     * Text box with a browse button; the chooser starts at the project relative path of the box and writes
     * the chosen path back relative to the project directory (absolute when outside of it)
     */
    @RequiredUIAccess
    public static TextBox createProjectRelativePathBox(Project project, FileChooserDescriptor descriptor, Disposable uiDisposable) {
        return FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(descriptor)
            .textBoxAccessor(new ProjectRelativePathAccessor(project))
            .disableCompletion()
            .uiDisposable(uiDisposable)
            .build()
            .getComponent();
    }

    /**
     * Small grayed hint shown below an option
     */
    @RequiredUIAccess
    public static Label createHintLabel(LocalizeValue text) {
        Label label = Label.create(text);
        label.setForegroundColor(ComponentColors.DISABLED_TEXT);
        return label;
    }

    /**
     * Puts {@code newItem} at the row of {@code oldItem}, appends it when the old one is gone
     */
    @RequiredUIAccess
    public static <T> void replaceItem(MutableFlatDataModel<T> model, T oldItem, T newItem) {
        int index = model.indexOf(oldItem);
        if (index < 0) {
            model.add(newItem);
            return;
        }

        model.remove(oldItem);
        model.add(newItem, index);
    }

    private static class ProjectRelativePathAccessor implements TextComponentAccessor<TextBox> {
        private final Project myProject;

        private ProjectRelativePathAccessor(Project project) {
            myProject = project;
        }

        @RequiredUIAccess
        @Override
        public String getValue(TextBox textBox) {
            String text = StringUtils.defaultString(textBox.getValue());

            VirtualFile projectDirectory = ProjectUtil.getProjectDir(myProject);
            if (projectDirectory == null) {
                return text;
            }

            VirtualFile relativeFile = text.isEmpty() ? null : VirtualFileUtil.findRelativeFile(text, projectDirectory);
            return relativeFile != null ? relativeFile.getPath() : projectDirectory.getPath();
        }

        @RequiredUIAccess
        @Override
        public void setValue(TextBox textBox, String text, boolean fireListeners) {
            String path = FileUtil.toSystemIndependentName(text);

            VirtualFile projectDirectory = ProjectUtil.getProjectDir(myProject);
            if (projectDirectory != null) {
                String basePath = projectDirectory.getPath();
                if (path.equals(basePath)) {
                    path = "";
                }
                else if (path.startsWith(basePath + "/")) {
                    path = path.substring(basePath.length() + 1);
                }
            }

            textBox.setValue(path, fireListeners);
        }
    }
}
