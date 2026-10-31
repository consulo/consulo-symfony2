package fr.adrienbrault.idea.symfony2plugin.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class SymfonyCommandSymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = scope instanceof ProjectAwareSearchScope projectAwareScope ? projectAwareScope.getProject() : null;
        if (project == null) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (SymfonyCommand command : SymfonyCommandUtil.getCommands(project)) {
            processor.process(command.getName());
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (SymfonyCommand command : SymfonyCommandUtil.getCommands(project)) {
            if (!name.equals(command.getName())) {
                continue;
            }

            PsiElement target = SymfonyCommandUtil.resolveCommandTarget(project, command);
            if (target != null) {
                processor.process(NavigationItemExStateless.create(target, name, Symfony2Icons.SYMFONY, "Command", true));
            }
        }
    }
}
