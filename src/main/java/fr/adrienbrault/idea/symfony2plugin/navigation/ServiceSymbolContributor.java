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
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceSymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = scope instanceof ProjectAwareSearchScope projectAwareScope ? projectAwareScope.getProject() : null;
        if (project == null) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        ContainerCollectionResolver.getServiceNames(project).forEach(processor::process);
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (PsiElement psiElement : ServiceIndexUtil.findServiceDefinitions(project, name)) {
            if (!(psiElement instanceof NavigationItem)) {
                continue;
            }

            processor.process(NavigationItemExStateless.create(psiElement, name, Symfony2Icons.SERVICE, "Service", true));
        }
    }
}
