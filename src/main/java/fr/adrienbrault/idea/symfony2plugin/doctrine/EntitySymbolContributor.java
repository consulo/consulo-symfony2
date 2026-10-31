package fr.adrienbrault.idea.symfony2plugin.doctrine;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class EntitySymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = ((GlobalSearchScope) scope).getProject();
        if (project == null) {
            return;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (DoctrineModel doctrineModel : EntityHelper.getModelClasses(project)) {
            processor.process(doctrineModel.getPhpClass().getName());
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        EntityHelper.getModelClasses(project).stream()
            .map(DoctrineModel::getPhpClass)
            .distinct()
            .filter(phpClass -> name.equals(phpClass.getName()))
            .forEach(phpClass -> processor.process(NavigationItemExStateless.create(phpClass, name, Symfony2Icons.DOCTRINE, "Entity", false)));
    }
}
