package fr.adrienbrault.idea.symfony2plugin.ux;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import com.jetbrains.twig.icons.TwigIcons;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class UxComponentSymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = ((GlobalSearchScope) scope).getProject();
        if (project == null) {
            return;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (UxUtil.TwigComponent twigComponent : UxUtil.getAllComponentNames(project)) {
            processor.process(twigComponent.name());
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (PhpClass component : UxUtil.getTwigComponentPhpClasses(project, name)) {
            processor.process(
                NavigationItemExStateless.create(
                    component,
                    name,
                    IconDescriptorUpdaters.getIcon(component, 0),
                    "TwigComponent (" + component.getName() + ")",
                    false
                )
            );
        }

        for (PsiFile component : UxUtil.getComponentTemplates(project, name)) {
            processor.process(
                NavigationItemExStateless.create(
                    component,
                    name,
                    TwigIcons.TwigFileIcon,
                    "TwigComponent (" + component.getName() + ")",
                    false
                )
            );
        }
    }
}
