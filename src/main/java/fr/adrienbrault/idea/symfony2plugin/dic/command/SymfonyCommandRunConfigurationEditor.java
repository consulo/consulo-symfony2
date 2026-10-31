package fr.adrienbrault.idea.symfony2plugin.dic.command;

import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfigurationEditor extends SettingsEditor<SymfonyCommandRunConfiguration> {
    private final ComboBox<SymfonyCommandRunConfiguration.ExecutionMode> myExecutionModeCombo =
        ComboBox.create(SymfonyCommandRunConfiguration.ExecutionMode.values());
    private final TextBox myCommandNameField = TextBox.create();
    private final TextBox myConsolePathField = TextBox.create();
    private final TextBox myInterpreterPathField = TextBox.create();
    private final TextBox mySymfonyCliPathField = TextBox.create();
    private final TextBox myWorkingDirectoryField = TextBox.create();
    private final TextBox myCommandLineParametersField = TextBox.create();

    private final Label myConsolePathLabel = Label.create(LocalizeValue.localizeTODO("Console path:"));
    private final Label myInterpreterPathLabel = Label.create(LocalizeValue.localizeTODO("PHP interpreter:"));
    private final Label mySymfonyCliPathLabel = Label.create(LocalizeValue.localizeTODO("Symfony CLI path:"));

    @RequiredUIAccess
    public SymfonyCommandRunConfigurationEditor() {
        myExecutionModeCombo.selectFirst();
        myExecutionModeCombo.addValueListener(event -> updateFieldVisibility());
    }

    @RequiredUIAccess
    private void updateFieldVisibility() {
        boolean isSymfonyCli = myExecutionModeCombo.getValue() == SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI;

        myConsolePathLabel.setVisible(!isSymfonyCli);
        myConsolePathField.setVisible(!isSymfonyCli);
        myInterpreterPathLabel.setVisible(!isSymfonyCli);
        myInterpreterPathField.setVisible(!isSymfonyCli);
        mySymfonyCliPathLabel.setVisible(isSymfonyCli);
        mySymfonyCliPathField.setVisible(isSymfonyCli);
    }

    @RequiredUIAccess
    @Override
    protected void resetEditorFrom(SymfonyCommandRunConfiguration configuration) {
        myExecutionModeCombo.setValue(configuration.getExecutionMode());
        myCommandNameField.setValue(nullToEmpty(configuration.getCommandName()));
        myConsolePathField.setValue(nullToDefault(configuration.getConsolePath(), "bin/console"));
        myInterpreterPathField.setValue(nullToDefault(configuration.getInterpreterPath(), "php"));
        mySymfonyCliPathField.setValue(nullToDefault(configuration.getSymfonyCliPath(), "symfony"));
        myWorkingDirectoryField.setValue(nullToEmpty(configuration.getWorkingDirectory()));
        myCommandLineParametersField.setValue(nullToEmpty(configuration.getCommandLineParameters()));
        updateFieldVisibility();
    }

    @RequiredUIAccess
    @Override
    protected void applyEditorTo(SymfonyCommandRunConfiguration configuration) {
        configuration.setExecutionMode(myExecutionModeCombo.getValueOrError());
        configuration.setCommandName(getTrimmedText(myCommandNameField));
        configuration.setConsolePath(getTrimmedText(myConsolePathField));
        configuration.setInterpreterPath(getTrimmedText(myInterpreterPathField));
        configuration.setSymfonyCliPath(getTrimmedText(mySymfonyCliPathField));
        configuration.setWorkingDirectory(getTrimmedText(myWorkingDirectoryField));
        configuration.setCommandLineParameters(getTrimmedText(myCommandLineParametersField));
    }

    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(LocalizeValue.localizeTODO("Execution mode:"), myExecutionModeCombo);
        builder.addLabeled(LocalizeValue.localizeTODO("Command:"), myCommandNameField);
        builder.addLabeled(myConsolePathLabel, myConsolePathField);
        builder.addLabeled(myInterpreterPathLabel, myInterpreterPathField);
        builder.addLabeled(mySymfonyCliPathLabel, mySymfonyCliPathField);
        builder.addLabeled(LocalizeValue.localizeTODO("Working directory:"), myWorkingDirectoryField);
        builder.addLabeled(LocalizeValue.localizeTODO("Additional parameters:"), myCommandLineParametersField);
        return builder.build();
    }

    private static String getTrimmedText(TextBox textBox) {
        return StringUtils.defaultString(textBox.getValue()).trim();
    }

    private static String nullToEmpty(@Nullable String value) {
        return value != null ? value : "";
    }

    private static String nullToDefault(@Nullable String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
