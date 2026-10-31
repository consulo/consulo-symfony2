package fr.adrienbrault.idea.symfony2plugin.runAnything;

import com.jetbrains.php.lang.PhpLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.dataContext.DataContext;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.ide.runAnything.RunAnythingCompletionGroup;
import consulo.ide.runAnything.RunAnythingGroup;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.ide.runAnything.RunAnythingItemBase;
import consulo.ide.runAnything.RunAnythingProviderBase;
import consulo.language.psi.PsiModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandRunConfiguration;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandRunConfigurationType;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Run Anything provider for Symfony console commands.
 * <p>
 * Provides completion and execution of Symfony console commands in the Run Anything popup
 * (Ctrl+Ctrl). Commands are discovered from project PHP classes using existing
 * {@link SymfonyCommandUtil} infrastructure.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "before RunAnythingCommandExecutionProvider")
public class SymfonyConsoleRunAnythingProvider extends RunAnythingProviderBase<SymfonyCommand> {
    private static final Key<CachedValue<List<SymfonyCommand>>> COMMAND_CACHE = Key.create("SYMFONY_RUN_ANYTHING_COMMANDS");

    @Override
    public Collection<SymfonyCommand> getValues(DataContext dataContext, String pattern) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return Collections.emptyList();
        }
        if (!Symfony2ProjectComponent.isEnabled(project) || DumbService.isDumb(project)) {
            return Collections.emptyList();
        }

        String lowerPattern = pattern.toLowerCase(Locale.ROOT).trim();

        List<SymfonyCommand> result = new ArrayList<>();
        for (SymfonyCommand command : getCommands(project)) {
            if (command.getName().toLowerCase(Locale.ROOT).contains(lowerPattern)) {
                result.add(command);
            }
        }
        return result;
    }

    private List<SymfonyCommand> getCommands(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            COMMAND_CACHE,
            () -> ReadAction.nonBlocking(() -> CachedValueProvider.Result.<List<SymfonyCommand>>create(
                    new ArrayList<>(SymfonyCommandUtil.getCommands(project)),
                    PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
                ))
                .expireWhen(project::isDisposed)
                .executeSynchronously(),
            false
        );
    }

    @Override
    public void execute(DataContext dataContext, SymfonyCommand value) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }

        RunManager runManager = RunManager.getInstance(project);
        SymfonyCommandRunConfigurationType type = SymfonyCommandRunConfigurationType.getInstance();

        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(value.getName(), type.getFactory());
        SymfonyCommandRunConfiguration configuration = (SymfonyCommandRunConfiguration) settings.getConfiguration();
        configuration.setCommandName(value.getName());

        // "symfony cli" or bin/console
        configuration.setExecutionMode(SymfonyCommandTestRunLineMarkerProvider.isSymfonyCliAvailable()
            ? SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI
            : SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER);

        configuration.setName(value.getName());

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        try {
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings)
                .buildAndExecute();
        }
        catch (ExecutionException e) {
            Symfony2ProjectComponent.getLogger().warn("Failed to execute Symfony command: " + value.getName(), e);
        }
    }

    // upstream override of IntelliJ RunAnythingProviderBase#getCompletionGroupTitle (absent in Consulo), see getCompletionGroup()
    public String getCompletionGroupTitle() {
        return "Symfony Console";
    }

    @Override
    public RunAnythingGroup getCompletionGroup() {
        return new RunAnythingCompletionGroup<>(this, LocalizeValue.localizeTODO(getCompletionGroupTitle()));
    }

    @Override
    public String getCommand(SymfonyCommand value) {
        return value.getName();
    }

    @Override
    public Image getIcon(SymfonyCommand value) {
        return Symfony2Icons.SYMFONY;
    }

    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, SymfonyCommand value) {
        String fqn = value.getFqn();
        String shortName = fqn.substring(fqn.lastIndexOf('\\') + 1);
        if (shortName.isEmpty()) {
            shortName = fqn;
        }
        String methodName = value.getMethodName();
        String description = methodName != null ? shortName + "::" + methodName : shortName;
        return new RunAnythingItemBase(value.getName(), Symfony2Icons.SYMFONY) {
            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    @Override
    public String getHelpGroupTitle() {
        return "Symfony";
    }
}
