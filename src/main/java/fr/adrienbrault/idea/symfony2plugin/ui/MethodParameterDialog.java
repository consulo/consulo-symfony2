package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodParameterDialog extends DialogDescriptor {
    private final Project myProject;
    private final @Nullable MethodParameterSetting myMethodParameterSetting;

    private final TextBox myTextCallTo = TextBox.create();
    private final TextBox myTextMethodName = TextBox.create();
    private final TextBox myTextIndex = TextBox.create();
    private final TextBox myTextContributorData = TextBox.create();
    private final ComboBox<String> myComboProvider;
    private final ComboBox<String> myComboContributor;

    @RequiredUIAccess
    private MethodParameterDialog(Project project, @Nullable MethodParameterSetting methodParameterSetting) {
        super(LocalizeValue.localizeTODO("MethodParameterSetting"));
        myProject = project;
        myMethodParameterSetting = methodParameterSetting;

        myComboProvider = ComboBox.create(List.of(AssistantReferenceUtil.getReferenceProvider(project)));
        myComboContributor = ComboBox.create(List.of(AssistantReferenceUtil.getContributorProvider(project)));
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myComboProvider.selectFirst();
        myComboContributor.selectFirst();

        if (myMethodParameterSetting != null) {
            myTextCallTo.setValue(StringUtils.defaultString(myMethodParameterSetting.getCallTo()));
            myTextMethodName.setValue(StringUtils.defaultString(myMethodParameterSetting.getMethodName()));
            myTextIndex.setValue(String.valueOf(myMethodParameterSetting.getIndexParameter()));
            myTextContributorData.setValue(StringUtils.defaultString(myMethodParameterSetting.getContributorData()));

            if (myMethodParameterSetting.getReferenceProviderName() != null) {
                myComboProvider.setValue(myMethodParameterSetting.getReferenceProviderName());
            }

            if (myMethodParameterSetting.getContributorName() != null) {
                myComboContributor.setValue(myMethodParameterSetting.getContributorName());
            }
        }

        myComboContributor.addValueListener(event -> updateContributorData());
        updateContributorData();

        myTextCallTo.addValueListener(event -> updateOkButtonState());
        myTextMethodName.addValueListener(event -> updateOkButtonState());

        // allow only number values to indexes
        myTextIndex.addValueListener(event -> {
            String text = StringUtils.defaultString(myTextIndex.getValue());
            String digits = text.replaceAll("[^0-9]", "");
            if (!digits.equals(text)) {
                myTextIndex.setValue(digits);
            }
            updateOkButtonState();
        });

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(LocalizeValue.localizeTODO("CallTo"), myTextCallTo);
        builder.addLabeled(LocalizeValue.localizeTODO("Method"), myTextMethodName);
        builder.addLabeled(LocalizeValue.localizeTODO("Index"), myTextIndex);
        builder.addLabeled(LocalizeValue.localizeTODO("Provider"), myComboProvider);
        builder.addLabeled(LocalizeValue.localizeTODO("Contributor"), myComboContributor);
        builder.addLabeled(LocalizeValue.localizeTODO("ContributorData"), myTextContributorData);
        return builder.build();
    }

    @RequiredUIAccess
    private void updateContributorData() {
        String contributor = myComboContributor.getValue();
        if (contributor == null) {
            return;
        }

        AssistantReferenceContributor assistantReferenceContributor = AssistantReferenceUtil.getContributorProviderByName(myProject, contributor);
        if (assistantReferenceContributor != null) {
            myTextContributorData.setEnabled(assistantReferenceContributor.supportData());
        }
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myTextCallTo;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return StringUtils.isNotEmpty(myTextCallTo.getValue())
            && StringUtils.isNotEmpty(myTextMethodName.getValue())
            && StringUtils.isNotEmpty(myTextIndex.getValue());
    }

    @RequiredUIAccess
    private MethodParameterSetting createSetting() {
        int index;
        try {
            index = Integer.parseInt(StringUtils.defaultString(myTextIndex.getValue()));
        }
        catch (NumberFormatException e) {
            index = 0;
        }

        String provider = myComboProvider.getValue();

        MethodParameterSetting setting = new MethodParameterSetting(
            StringUtils.defaultString(myTextCallTo.getValue()),
            StringUtils.defaultString(myTextMethodName.getValue()),
            index,
            provider
        );
        setting.setContributorName(myComboContributor.getValue());

        if (myTextContributorData.isEnabled()) {
            setting.setContributorData(myTextContributorData.getValue());
        }

        setting.setReferenceProviderName(provider);
        return setting;
    }

    /**
     * @param methodParameterSetting {@code null} to create a new setting
     * @param onOk receives a new setting instance with the dialog values
     */
    @RequiredUIAccess
    public static void show(Project project, @Nullable MethodParameterSetting methodParameterSetting, Consumer<MethodParameterSetting> onOk) {
        MethodParameterDialog descriptor = new MethodParameterDialog(project, methodParameterSetting);

        UIAccess uiAccess = UIAccess.current();
        project.getApplication().getInstance(DialogService.class)
            .build(project, descriptor)
            .showAsync()
            .whenCompleteAsync((value, error) -> {
                if (error == null && value != null) {
                    onOk.accept(descriptor.createSetting());
                }
            }, uiAccess);
    }
}
