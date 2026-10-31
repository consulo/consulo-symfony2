package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.jetbrains.yaml.YAMLLanguage;

/**
 * Registers {@link ServiceArgumentParameterHintsProvider} for YAML (upstream registered the same class for XML and yaml).
 */
@ExtensionImpl
public class ServiceArgumentYamlParameterHintsProvider extends ServiceArgumentParameterHintsProvider {
    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
