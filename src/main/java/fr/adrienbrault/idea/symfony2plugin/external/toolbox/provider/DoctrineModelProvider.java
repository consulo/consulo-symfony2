package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import consulo.ui.image.Image;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderInterface;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelProvider implements PhpToolboxProviderInterface { // TODO consulo-port: was registered as de.espend.idea.php.toolbox PhpToolboxProviderInterface (optional toolbox.xml); no Consulo toolbox EP yet
    @Override
    public Collection<LookupElement> getLookupElements(PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return EntityReference.getModelLookupElements(parameter.getProject());
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return Arrays.asList(
            EntityHelper.getModelPsiTargets(parameter.getProject(), parameter.getContents())
        );
    }

    @Override
    public String getName() {
        return "doctrine.models";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Image getIcon() {
                return Symfony2Icons.DOCTRINE;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Doctrine entities, documents, ...";
            }
        };
    }
}
