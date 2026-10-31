package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.util.lang.Pair;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class EntityTableSymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        Project project = ((GlobalSearchScope) scope).getProject();
        if (project == null) {
            return;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Pair<String, String> table : DoctrineMetadataUtil.getTables(project)) {
            processor.process(table.first);
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Pair<String, String> table : DoctrineMetadataUtil.getTables(project)) {
            if (!name.equals(table.first)) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, table.second);
            if (phpClass != null) {
                processor.process(
                    NavigationItemExStateless.create(phpClass, table.first, Symfony2Icons.DOCTRINE, "Entity Table", false)
                );
            }
        }
    }
}
