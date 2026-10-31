package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
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
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * "Method References" section of the Symfony module extension panel
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodParameterReferenceSettingsForm {
    private final Project myProject;
    private final Settings mySettings;

    public MethodParameterReferenceSettingsForm(Project project, Settings settings) {
        myProject = project;
        mySettings = settings;
    }

    @RequiredUIAccess
    public Component createComponent() {
        List<MethodParameterSetting> initialItems = mySettings.methodParameterSettings;
        MutableFlatDataModel<MethodParameterSetting> model = FlatDataModel.of(initialItems != null ? initialItems : List.of());
        model.addListener(event -> {
            List<MethodParameterSetting> settings = new ArrayList<>();
            for (MethodParameterSetting setting : model) {
                settings.add(setting);
            }
            mySettings.methodParameterSettings = settings;
        });

        Table<MethodParameterSetting> table = Table.create(model);
        table.addColumn(LocalizeValue.localizeTODO("CallTo"), item -> StringUtils.defaultString(item.getCallTo()));
        table.addColumn(LocalizeValue.localizeTODO("Method"), item -> StringUtils.defaultString(item.getMethodName()));
        table.addColumn(LocalizeValue.localizeTODO("Index"), MethodParameterSetting::getIndexParameter);
        table.addColumn(LocalizeValue.localizeTODO("Provider"), item -> StringUtils.defaultString(item.getReferenceProviderName()));
        table.addColumn(LocalizeValue.localizeTODO("Contributor"), item -> StringUtils.defaultString(item.getContributorName()));
        table.addColumn(LocalizeValue.localizeTODO("ContributorData"), item -> StringUtils.defaultString(item.getContributorData()));

        Component tablePanel = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    MethodParameterDialog.show(myProject, null, setting -> {
                        model.add(setting);
                        table.select(setting);
                    });
                }
            })
            .addOrReplaceAction(new EditAction<>() {
                @RequiredUIAccess
                @Override
                protected void doEdit(MethodParameterSetting value, AnActionEvent e) {
                    MethodParameterDialog.show(myProject, value, setting -> {
                        UiSettingsUtil.replaceItem(model, value, setting);
                        table.select(setting);
                    });
                }
            })
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();

        Button buttonHelp = Button.create(
            LocalizeValue.localizeTODO("Help"),
            event -> IdeHelper.openUrl("https://www.jetbrains.com/help/phpstorm/symfony-creating-helper-functions.html#method-parameter")
        );

        DockLayout header = DockLayout.create();
        header.center(Label.create(LocalizeValue.localizeTODO("Provides Completion and Goto for method parameter")));
        header.right(buttonHelp);

        DockLayout root = DockLayout.create();
        root.top(header);
        root.center(tablePanel);
        return root;
    }
}
