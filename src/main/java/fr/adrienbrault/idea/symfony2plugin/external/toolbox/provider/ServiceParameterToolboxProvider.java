package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import consulo.annotation.component.ExtensionImpl;

import consulo.ui.image.Image;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderAbstract;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceParameterToolboxProvider extends PhpToolboxProviderAbstract {
    @Override
    public Collection<LookupElement> getLookupElements(PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        List<LookupElement> results = new ArrayList<>();

        for(Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(parameter.getProject()).entrySet()) {
            results.add(new ParameterLookupElement(entry.getValue()));
        }

        return results;
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "symfony.parameters";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Image getIcon() {
                return Symfony2Icons.PARAMETER;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Container parameter";
            }
        };
    }
}
