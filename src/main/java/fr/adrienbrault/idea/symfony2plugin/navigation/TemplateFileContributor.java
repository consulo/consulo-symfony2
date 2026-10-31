package fr.adrienbrault.idea.symfony2plugin.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigation.GotoFileContributor;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import com.jetbrains.twig.icons.TwigIcons;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TemplateFileContributor implements GotoFileContributor {
    @Override
    public String[] getNames(Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new String[0];
        }

        Collection<String> twigFileNames = TwigUtil.getTemplateMap(project).keySet();
        return twigFileNames.toArray(new String[0]);
    }

    @Override
    public NavigationItem[] getItemsByName(String templateName, String s2, Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new NavigationItem[0];
        }

        return TwigUtil.getTemplatePsiElements(project, templateName)
            .stream()
            .map(psiFile ->
                new NavigationItemEx(psiFile, templateName, TwigIcons.TwigFileIcon, "Template", false)
            )
            .toArray(NavigationItemEx[]::new);
    }
}
