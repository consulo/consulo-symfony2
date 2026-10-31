package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.codeEditor.Caret;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunAction extends AnAction {

    private final SymfonyCommandRunConfiguration.ExecutionMode myMode;

    public SymfonyCommandRunAction(SymfonyCommandRunConfiguration.ExecutionMode mode, String text, Image icon) {
        super(LocalizeValue.of(text), LocalizeValue.empty(), icon);
        this.myMode = mode;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        PsiElement psiElement = e.getData(PsiElement.KEY);
        if (psiElement == null) {
            PsiFile psiFile = e.getData(PsiFile.KEY);
            if (psiFile != null) {
                psiElement = psiFile.findElementAt(e.getData(Caret.KEY) != null ? e.getData(Caret.KEY).getOffset() : 0);
            }
        }

        if (psiElement == null) {
            return;
        }

        PhpNamedElement commandTarget;
        if (psiElement instanceof PhpNamedElement phpNamedElement) {
            commandTarget = phpNamedElement;
        } else {
            commandTarget = SymfonyCommandTestRunLineMarkerProvider.getCommandTargetContext(psiElement);
            if (commandTarget == null && psiElement.getParent() instanceof PhpNamedElement phpNamedElement) {
                commandTarget = phpNamedElement;
            }
        }

        if (commandTarget == null) {
            return;
        }

        List<String> commandNames = SymfonyCommandTestRunLineMarkerProvider.getCommandNames(commandTarget);
        if (commandNames.isEmpty()) {
            return;
        }

        String commandName = commandNames.getFirst();

        RunManager runManager = RunManager.getInstance(project);
        SymfonyCommandRunConfigurationType type = SymfonyCommandRunConfigurationType.getInstance();

        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(commandName, type.getFactory());
        SymfonyCommandRunConfiguration configuration = (SymfonyCommandRunConfiguration) settings.getConfiguration();
        configuration.setCommandName(commandName);
        configuration.setExecutionMode(myMode);
        configuration.setName(commandName);

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        try {
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings)
                .buildAndExecute();
        } catch (ExecutionException ignored) {
        }
    }
}
