package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * "Type Provider" section of the Symfony module extension panel
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeSettingsForm {
    private final Project myProject;
    private final Settings mySettings;

    public MethodSignatureTypeSettingsForm(Project project, Settings settings) {
        myProject = project;
        mySettings = settings;
    }

    @RequiredUIAccess
    public Component createComponent() {
        List<MethodSignatureSetting> initialItems = mySettings.methodSignatureSettings;
        MutableFlatDataModel<MethodSignatureSetting> model = FlatDataModel.of(initialItems != null ? initialItems : List.of());
        model.addListener(event -> {
            List<MethodSignatureSetting> settings = new ArrayList<>();
            for (MethodSignatureSetting setting : model) {
                settings.add(setting);
            }
            mySettings.methodSignatureSettings = settings;
        });

        Table<MethodSignatureSetting> table = Table.create(model);
        table.addColumn(LocalizeValue.localizeTODO("CallTo"), item -> StringUtils.defaultString(item.getCallTo()));
        table.addColumn(LocalizeValue.localizeTODO("Method"), item -> StringUtils.defaultString(item.getMethodName()));
        table.addColumn(LocalizeValue.localizeTODO("Index"), MethodSignatureSetting::getIndexParameter);
        table.addColumn(LocalizeValue.localizeTODO("Provider"), item -> StringUtils.defaultString(item.getReferenceProviderName()));

        Component tablePanel = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    MethodSignatureTypeDialog.show(myProject, null, setting -> {
                        model.add(setting);
                        table.select(setting);
                    });
                }
            })
            .addOrReplaceAction(new EditAction<>() {
                @RequiredUIAccess
                @Override
                protected void doEdit(MethodSignatureSetting value, AnActionEvent e) {
                    MethodSignatureTypeDialog.show(myProject, value, setting -> {
                        UiSettingsUtil.replaceItem(model, value, setting);
                        table.select(setting);
                    });
                }
            })
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();

        CheckBox enableCustomSignatureTypesCheckBox = CheckBox.create(
            LocalizeValue.localizeTODO("Enable Custom Signature Types"),
            mySettings.objectSignatureTypeProvider
        );
        enableCustomSignatureTypesCheckBox.addValueListener(event -> mySettings.objectSignatureTypeProvider = event.getValue());

        Button buttonHelp = Button.create(
            LocalizeValue.localizeTODO("Help"),
            event -> IdeHelper.openUrl("https://www.jetbrains.com/help/phpstorm/symfony-creating-helper-functions.html#signature-types")
        );

        DockLayout header = DockLayout.create();
        header.center(enableCustomSignatureTypesCheckBox);
        header.right(buttonHelp);

        DockLayout root = DockLayout.create();
        root.top(header);
        root.center(tablePanel);
        return root;
    }
}
