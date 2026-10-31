package fr.adrienbrault.idea.symfony2plugin.dic.command;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class SymfonyCommandRunConfigurationType extends ConfigurationTypeBase implements DumbAware {

    public static final String ID = "SymfonyCommandRunConfigurationType";

    public SymfonyCommandRunConfigurationType() {
        super(ID, LocalizeValue.localizeTODO("Symfony Command"), LocalizeValue.localizeTODO("Run Symfony console commands"), Symfony2Icons.SYMFONY);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new SymfonyCommandRunConfiguration(project, this, "Symfony Command");
            }

            @Override
            public String getId() {
                return "Symfony Command";
            }
        });
    }

    public static SymfonyCommandRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(SymfonyCommandRunConfigurationType.class);
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }
}
