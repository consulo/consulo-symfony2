package fr.adrienbrault.idea.symfony2plugin.external.locator;

import consulo.annotation.component.ExtensionImpl;

import consulo.language.psi.PsiElement;
import de.espend.idea.php.toolbox.extension.PhpToolboxTargetLocator;
import de.espend.idea.php.toolbox.navigation.locator.TargetLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class BundleFileToolboxTargetLocator implements PhpToolboxTargetLocator {
    @Override
    public Collection<PsiElement> getTargets(TargetLocatorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        String target = parameter.getTarget();
        if(!target.startsWith("@")) {
            return Collections.emptyList();
        }

        return new ArrayList<>(FileResourceUtil.getFileResourceTargetsInBundleScope(parameter.getProject(), parameter.getTarget()));
    }
}
