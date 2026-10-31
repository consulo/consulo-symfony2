package fr.adrienbrault.idea.symfony2plugin.external.locator;

import consulo.annotation.component.ExtensionImpl;

import consulo.language.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigFileToolboxTargetLocator implements PhpToolboxTargetLocator {
    @Override
    public Collection<PsiElement> getTargets(TargetLocatorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        String target = parameter.getTarget();
        if(!target.toLowerCase().endsWith(".twig")) {
            return Collections.emptyList();
        }

        return new HashSet<>(TwigUtil.getTemplatePsiElements(parameter.getProject(), target));
    }
}
