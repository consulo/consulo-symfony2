package fr.adrienbrault.idea.symfony2plugin.dic.command;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersListUtil;
import consulo.process.local.ProcessHandlerFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfiguration extends LocatableConfigurationBase {

    public enum ExecutionMode {
        SYMFONY_CLI("Symfony CLI"),
        PHP_INTERPRETER("PHP Interpreter");

        private final String displayName;

        ExecutionMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private ExecutionMode executionMode = ExecutionMode.SYMFONY_CLI;
    private String commandName;
    private String consolePath = "bin/console";
    private String workingDirectory;
    private String interpreterPath = "php";
    private String symfonyCliPath = "symfony";
    private String commandLineParameters;

    protected SymfonyCommandRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    // TODO consulo-port: RunConfiguration#getConfigurationEditor() does not exist in Consulo; the editor must be provided via RunConfiguration#createFragments() (RunConfigurationFragment)
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SymfonyCommandRunConfigurationEditor();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (commandName == null || commandName.isEmpty()) {
            throw new RuntimeConfigurationError("Command name is required");
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
        return (executor1, runner) -> {
            Project project = getProject();
            VirtualFile projectDir = ProjectUtil.getProjectDir(project);

            String workDir = workingDirectory != null && !workingDirectory.isEmpty()
                ? workingDirectory
                : projectDir.getPath();

            List<String> command = new ArrayList<>();

            if (executionMode == ExecutionMode.SYMFONY_CLI) {
                String cli = symfonyCliPath != null && !symfonyCliPath.isEmpty() ? symfonyCliPath : "symfony";
                command.add(cli);
                command.add("console");
            } else {
                String interpreter = interpreterPath != null && !interpreterPath.isEmpty() ? interpreterPath : "php";
                String console = consolePath != null && !consolePath.isEmpty() ? consolePath : "bin/console";
                command.add(interpreter);
                command.add(console);
            }

            command.add(commandName);

            if (commandLineParameters != null && !commandLineParameters.isEmpty()) {
                command.addAll(ParametersListUtil.parse(commandLineParameters));
            }

            GeneralCommandLine commandLine = new GeneralCommandLine();
            commandLine.setExePath(command.get(0));
            commandLine.setPassParentEnvironment(true);
            commandLine.addParameters(command.subList(1, command.size()).toArray(new String[0]));
            commandLine.setWorkDirectory(workDir);

            // TODO consulo-port: KillableProcessHandler#setShouldKillProcessSoftly(false) has no Consulo counterpart
            ProcessHandler processHandler = ProcessHandlerFactory.getInstance().createKillableProcessHandler(commandLine);

            ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            consoleView.attachToProcess(processHandler);
            return new DefaultExecutionResult(consoleView, processHandler);
        };
    }

    @Override
    public void readExternal(Element element) {
        super.readExternal(element);
        Element settingsElement = element.getChild("symfony-command-settings");
        if (settingsElement != null) {
            commandName = settingsElement.getAttributeValue("commandName");
            consolePath = settingsElement.getAttributeValue("consolePath", "bin/console");
            workingDirectory = settingsElement.getAttributeValue("workingDirectory");
            interpreterPath = settingsElement.getAttributeValue("interpreterPath", "php");
            symfonyCliPath = settingsElement.getAttributeValue("symfonyCliPath", "symfony");
            commandLineParameters = settingsElement.getAttributeValue("commandLineParameters");
            String mode = settingsElement.getAttributeValue("executionMode");
            if (mode != null) {
                try {
                    executionMode = ExecutionMode.valueOf(mode);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @Override
    public void writeExternal(Element element) {
        super.writeExternal(element);
        Element settingsElement = new Element("symfony-command-settings");
        settingsElement.setAttribute("executionMode", executionMode.name());
        if (commandName != null) settingsElement.setAttribute("commandName", commandName);
        if (consolePath != null) settingsElement.setAttribute("consolePath", consolePath);
        if (workingDirectory != null) settingsElement.setAttribute("workingDirectory", workingDirectory);
        if (interpreterPath != null) settingsElement.setAttribute("interpreterPath", interpreterPath);
        if (symfonyCliPath != null) settingsElement.setAttribute("symfonyCliPath", symfonyCliPath);
        if (commandLineParameters != null) settingsElement.setAttribute("commandLineParameters", commandLineParameters);
        element.addContent(settingsElement);
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    @Nullable
    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(@Nullable String commandName) {
        this.commandName = commandName;
    }

    @Nullable
    public String getConsolePath() {
        return consolePath;
    }

    public void setConsolePath(@Nullable String consolePath) {
        this.consolePath = consolePath;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Nullable
    public String getInterpreterPath() {
        return interpreterPath;
    }

    public void setInterpreterPath(@Nullable String interpreterPath) {
        this.interpreterPath = interpreterPath;
    }

    @Nullable
    public String getSymfonyCliPath() {
        return symfonyCliPath;
    }

    public void setSymfonyCliPath(@Nullable String symfonyCliPath) {
        this.symfonyCliPath = symfonyCliPath;
    }

    @Nullable
    public String getCommandLineParameters() {
        return commandLineParameters;
    }

    public void setCommandLineParameters(@Nullable String commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
    }
}
