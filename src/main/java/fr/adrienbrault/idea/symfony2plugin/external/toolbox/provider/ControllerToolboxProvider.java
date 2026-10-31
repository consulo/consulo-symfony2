package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import consulo.annotation.component.ExtensionImpl;

import consulo.ui.image.Image;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import com.jetbrains.php.PhpIcons;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderInterface;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ControllerToolboxProvider implements PhpToolboxProviderInterface {
    @Override
    public Collection<LookupElement> getLookupElements(PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return ControllerIndex.getControllerLookupElements(parameter.getProject());
    }

    @Override
    public Collection<PsiElement> getPsiTargets(final PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(parameter.getProject(), parameter.getContents()));
    }

    @Override
    public String getName() {
        return "symfony.controller";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Image getIcon() {
                return PhpIcons.METHOD;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Controller";
            }
        };
    }
}
