package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.virtualFileSystem.fileType.FileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.UiPathColumnInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Settings section with an editable list of project relative files ("Path" / "Info" table with a "Reset To Default" button).
 * Every change is written into the bound {@link Settings} bean.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public abstract class UiFilePathSettingsForm<T extends UiFilePathInterface> {
    protected final Project myProject;
    protected final Settings mySettings;

    protected UiFilePathSettingsForm(Project project, Settings settings) {
        myProject = project;
        mySettings = settings;
    }

    /**
     * File type offered by the chooser of the add / edit actions
     */
    protected abstract FileType getFileType();

    protected abstract T createItem(String path);

    protected abstract Collection<T> getSettingsItems();

    protected abstract Collection<String> getDefaultPaths();

    protected abstract void applyItems(List<T> items);

    /**
     * Description shown left of the "Reset To Default" button
     */
    @RequiredUIAccess
    protected abstract Component createDescription();

    @RequiredUIAccess
    public Component createComponent() {
        List<T> initialItems = new ArrayList<>();
        for (T item : getSettingsItems()) {
            initialItems.add(createItem(item.getPath()));
        }

        MutableFlatDataModel<T> model = FlatDataModel.of(initialItems);
        model.addListener(event -> {
            List<T> items = new ArrayList<>();
            for (T item : model) {
                items.add(createItem(item.getPath()));
            }
            applyItems(items);
        });

        Table<T> table = Table.create(model);
        UiPathColumnInfo.addColumns(table, myProject);

        Component tablePanel = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    UiSettingsUtil.getPathDialog(myProject, getFileType()).whenCompleteAsync((uri, error) -> {
                        if (uri != null) {
                            model.add(createItem(uri));
                        }
                    }, myProject.getUIAccess());
                }
            })
            .addOrReplaceAction(new EditAction<>() {
                @RequiredUIAccess
                @Override
                protected void doEdit(T value, AnActionEvent e) {
                    UiSettingsUtil.getPathDialog(myProject, getFileType()).whenCompleteAsync((uri, error) -> {
                        if (uri != null) {
                            value.setPath(uri);
                            model.update(value);
                        }
                    }, myProject.getUIAccess());
                }
            })
            .build();

        Button buttonReset = Button.create(LocalizeValue.localizeTODO("Reset To Default"), event -> {
            List<T> items = new ArrayList<>();
            for (String path : getDefaultPaths()) {
                items.add(createItem(path));
            }
            model.replaceAll(items);
        });

        DockLayout header = DockLayout.create();
        header.center(createDescription());
        header.right(buttonReset);

        DockLayout root = DockLayout.create();
        root.top(header);
        root.center(tablePanel);
        return root;
    }
}
