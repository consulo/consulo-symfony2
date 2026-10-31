package fr.adrienbrault.idea.symfony2plugin.dic.command;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * The settings of a Symfony command run configuration: how the console is started, the command and its parameters
 */
public class SymfonyCommandRunConfigurationFragment implements RunConfigurationFragment<SymfonyCommandRunConfiguration> {
    @Override
    public String getId() {
        return "symfonyCommand";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony command");
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SymfonyCommandRunConfiguration> createEditor(Project project, Disposable parent) {
        return new Editor();
    }

    private static class Editor implements RunConfigurationFragmentEditor<SymfonyCommandRunConfiguration> {
        private final ComboBox<SymfonyCommandRunConfiguration.ExecutionMode> myExecutionModeCombo =
            ComboBox.create(SymfonyCommandRunConfiguration.ExecutionMode.values());
        private final TextBox myCommandNameField = TextBox.create();
        private final TextBox myConsolePathField = TextBox.create();
        private final TextBox myInterpreterPathField = TextBox.create();
        private final TextBox mySymfonyCliPathField = TextBox.create();
        private final TextBox myWorkingDirectoryField = TextBox.create();
        private final TextBox myCommandLineParametersField = TextBox.create();

        @RequiredUIAccess
        private Editor() {
            myExecutionModeCombo.selectFirst();
            myExecutionModeCombo.addValueListener(event -> updateFieldVisibility());
        }

        @RequiredUIAccess
        @Override
        public void addRows(RunConfigurationFragmentLayout layout) {
            layout.addLabeled(LocalizeValue.localizeTODO("Execution mode:"), myExecutionModeCombo);
            layout.addLabeled(LocalizeValue.localizeTODO("Command:"), myCommandNameField);
            layout.addLabeled(LocalizeValue.localizeTODO("Console path:"), myConsolePathField);
            layout.addLabeled(LocalizeValue.localizeTODO("PHP interpreter:"), myInterpreterPathField);
            layout.addLabeled(LocalizeValue.localizeTODO("Symfony CLI path:"), mySymfonyCliPathField);
            layout.addLabeled(LocalizeValue.localizeTODO("Working directory:"), myWorkingDirectoryField);
            layout.addLabeled(LocalizeValue.localizeTODO("Additional parameters:"), myCommandLineParametersField);
            updateFieldVisibility();
        }

        @RequiredUIAccess
        @Override
        public void reset(SymfonyCommandRunConfiguration configuration) {
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
        public void apply(SymfonyCommandRunConfiguration configuration) {
            configuration.setExecutionMode(myExecutionModeCombo.getValueOrError());
            configuration.setCommandName(getTrimmedText(myCommandNameField));
            configuration.setConsolePath(getTrimmedText(myConsolePathField));
            configuration.setInterpreterPath(getTrimmedText(myInterpreterPathField));
            configuration.setSymfonyCliPath(getTrimmedText(mySymfonyCliPathField));
            configuration.setWorkingDirectory(getTrimmedText(myWorkingDirectoryField));
            configuration.setCommandLineParameters(getTrimmedText(myCommandLineParametersField));
        }

        /**
         * The paths of the other execution mode are disabled: a row can not be hidden with its label
         */
        @RequiredUIAccess
        private void updateFieldVisibility() {
            boolean isSymfonyCli = myExecutionModeCombo.getValue() == SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI;
            myConsolePathField.setEnabled(!isSymfonyCli);
            myInterpreterPathField.setEnabled(!isSymfonyCli);
            mySymfonyCliPathField.setEnabled(isSymfonyCli);
        }
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
