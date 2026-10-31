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
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.PhpTypeSignatureInterface;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.PhpTypeSignatureTypes;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeDialog extends DialogDescriptor {
    private final @Nullable MethodSignatureSetting myMethodSignatureSetting;

    private final TextBox myTextCallTo = TextBox.create();
    private final TextBox myTextMethodName = TextBox.create();
    private final TextBox myTextIndex = TextBox.create();
    private final ComboBox<String> myComboProvider;

    @RequiredUIAccess
    private MethodSignatureTypeDialog(@Nullable MethodSignatureSetting methodSignatureSetting) {
        super(LocalizeValue.localizeTODO("MethodSignatureSetting"));
        myMethodSignatureSetting = methodSignatureSetting;

        List<String> providers = new ArrayList<>();
        for (PhpTypeSignatureInterface provider : PhpTypeSignatureTypes.DEFAULT_PROVIDER) {
            providers.add(provider.getName());
        }
        myComboProvider = ComboBox.create(providers);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myComboProvider.selectFirst();

        if (myMethodSignatureSetting != null) {
            myTextCallTo.setValue(StringUtils.defaultString(myMethodSignatureSetting.getCallTo()));
            myTextMethodName.setValue(StringUtils.defaultString(myMethodSignatureSetting.getMethodName()));
            myTextIndex.setValue(String.valueOf(myMethodSignatureSetting.getIndexParameter()));

            if (myMethodSignatureSetting.getReferenceProviderName() != null) {
                myComboProvider.setValue(myMethodSignatureSetting.getReferenceProviderName());
            }
        }

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
        return builder.build();
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
    private MethodSignatureSetting createSetting() {
        int index;
        try {
            index = Integer.parseInt(StringUtils.defaultString(myTextIndex.getValue()));
        }
        catch (NumberFormatException e) {
            index = 0;
        }

        return new MethodSignatureSetting(
            StringUtils.defaultString(myTextCallTo.getValue()),
            StringUtils.defaultString(myTextMethodName.getValue()),
            index,
            myComboProvider.getValue()
        );
    }

    /**
     * @param methodSignatureSetting {@code null} to create a new setting
     * @param onOk receives a new setting instance with the dialog values
     */
    @RequiredUIAccess
    public static void show(Project project, @Nullable MethodSignatureSetting methodSignatureSetting, Consumer<MethodSignatureSetting> onOk) {
        MethodSignatureTypeDialog descriptor = new MethodSignatureTypeDialog(methodSignatureSetting);

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
