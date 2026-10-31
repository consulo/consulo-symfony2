package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigNamespaceDialog extends DialogDescriptor {
    private final Project myProject;
    private final @Nullable TwigPath myTwigPath;

    private final TextBox myName = TextBox.create();
    private final ComboBox<TwigUtil.NamespaceType> myNamespaceType = ComboBox.create(TwigUtil.NamespaceType.ADD_PATH, TwigUtil.NamespaceType.BUNDLE);
    private final CheckBox myChkboxEnabled = CheckBox.create(LocalizeValue.localizeTODO("Enabled"), true);
    private @Nullable TextBox myNamespacePath;
    private @Nullable Label myErrorLabel;

    @RequiredUIAccess
    private TwigNamespaceDialog(Project project, @Nullable TwigPath twigPath) {
        super(LocalizeValue.localizeTODO("Twig Namespace"));
        myProject = project;
        myTwigPath = twigPath;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TextBox namespacePath = UiSettingsUtil.createProjectRelativePathBox(
            myProject,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            uiDisposable
        );
        myNamespacePath = namespacePath;

        myNamespaceType.selectFirst();

        if (myTwigPath != null) {
            myName.setValue(StringUtils.defaultString(myTwigPath.getNamespace()));
            namespacePath.setValue(StringUtils.defaultString(myTwigPath.getPath()));
            myNamespaceType.setValue(myTwigPath.getNamespaceType());
        }

        namespacePath.addValueListener(event -> updateState());
        myName.addValueListener(event -> updateState());

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(LocalizeValue.localizeTODO("Namespace"), myName);
        builder.addLabeled(LocalizeValue.localizeTODO("Path (relative to project)"), namespacePath);
        builder.addLabeled(LocalizeValue.localizeTODO("Type"), myNamespaceType);
        builder.addBottom(myChkboxEnabled);

        Label errorLabel = Label.create(LocalizeValue.empty());
        errorLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        myErrorLabel = errorLabel;

        DockLayout layout = DockLayout.create();
        layout.center(builder.build());
        layout.bottom(errorLabel);

        updateState();
        return layout;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myName;
    }

    private String getNamespacePath() {
        TextBox namespacePath = myNamespacePath;
        return namespacePath != null ? StringUtils.defaultString(namespacePath.getValue()) : "";
    }

    @RequiredUIAccess
    private void updateState() {
        Label errorLabel = myErrorLabel;
        if (errorLabel != null) {
            // the chooser writes a path outside of the project as absolute one
            boolean outsideProject = FileUtil.isAbsolute(getNamespacePath());
            errorLabel.setText(outsideProject
                ? LocalizeValue.localizeTODO("Selected directory is outside the project root. Please select a directory within the project.")
                : LocalizeValue.empty()
            );
            errorLabel.setVisible(outsideProject);
        }

        updateOkButtonState();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        String namespacePath = getNamespacePath();
        return !namespacePath.isEmpty() && !FileUtil.isAbsolute(namespacePath);
    }

    @RequiredUIAccess
    private TwigPath createTwigPath() {
        // empty namespace use "__main__", as this is same as Twig internals does
        String namespace = myName.getValue();
        if (StringUtils.isBlank(namespace)) {
            namespace = "__main__";
        }

        TwigUtil.NamespaceType namespaceType = myNamespaceType.getValue();

        return TwigPath.createTwigPath(
            getNamespacePath(),
            namespace,
            namespaceType != null ? namespaceType : TwigUtil.NamespaceType.ADD_PATH,
            true,
            myChkboxEnabled.getValueOrError()
        );
    }

    /**
     * @param twigPath {@code null} to create a new namespace
     * @param onOk receives the new twig path with the dialog values
     */
    @RequiredUIAccess
    public static void show(Project project, @Nullable TwigPath twigPath, Consumer<TwigPath> onOk) {
        TwigNamespaceDialog descriptor = new TwigNamespaceDialog(project, twigPath);

        UIAccess uiAccess = UIAccess.current();
        project.getApplication().getInstance(DialogService.class)
            .build(project, descriptor)
            .showAsync()
            .whenCompleteAsync((value, error) -> {
                if (error == null && value != null && !descriptor.getNamespacePath().isEmpty()) {
                    onOk.accept(descriptor.createTwigPath());
                }
            }, uiAccess);
    }
}
