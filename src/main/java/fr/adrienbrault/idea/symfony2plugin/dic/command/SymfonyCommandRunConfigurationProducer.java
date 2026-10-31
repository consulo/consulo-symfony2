package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.action.RunConfigurationProducer;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.SimpleReference;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class SymfonyCommandRunConfigurationProducer extends RunConfigurationProducer<SymfonyCommandRunConfiguration> {

    public SymfonyCommandRunConfigurationProducer() {
        super(SymfonyCommandRunConfigurationType.getInstance().getFactory());
    }

    @Override
    protected boolean setupConfigurationFromContext(SymfonyCommandRunConfiguration configuration, ConfigurationContext context, SimpleReference<PsiElement> sourceElement) {
        List<String> commandNames = getCommandNames(context);
        if (commandNames.isEmpty()) {
            return false;
        }

        String commandName = commandNames.get(0);
        configuration.setCommandName(commandName);
        configuration.setName(commandName);

        if (SymfonyCommandTestRunLineMarkerProvider.isSymfonyCliAvailable()) {
            configuration.setExecutionMode(SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI);
        } else {
            configuration.setExecutionMode(SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER);
        }

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(SymfonyCommandRunConfiguration configuration, ConfigurationContext context) {
        List<String> commandNames = getCommandNames(context);
        if (commandNames.isEmpty()) {
            return false;
        }

        return commandNames.contains(configuration.getCommandName());
    }

    private static List<String> getCommandNames(ConfigurationContext context) {
        Location<?> location = context.getLocation();
        if (!(location instanceof PsiLocation)) {
            return List.of();
        }

        PhpNamedElement commandTarget = SymfonyCommandTestRunLineMarkerProvider.getCommandTargetContext(location.getPsiElement());
        if (commandTarget == null) {
            return List.of();
        }

        return SymfonyCommandTestRunLineMarkerProvider.getCommandNames(commandTarget);
    }
}
