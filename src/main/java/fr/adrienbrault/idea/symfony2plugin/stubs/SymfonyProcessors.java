package fr.adrienbrault.idea.symfony2plugin.stubs;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.application.util.function.Processor;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.index.io.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProcessors {

    @Deprecated
    public static class CollectProjectUniqueKeys implements Processor<String> {
        final Project project;

        final ID<String, ?>  id;

        final Set<String> stringSet;

        @Deprecated
        public CollectProjectUniqueKeys(Project project, ID<String, ?>  id) {
            this.project = project;
            this.id = id;
            this.stringSet = new HashSet<>();
        }

        @Override
        public boolean process(String s) {
            this.stringSet.add(s);
            return true;
        }

        public Set<String> getResult() {
            return stringSet.stream()
                .filter(
                    s -> !FileBasedIndex.getInstance().getContainingFiles(id, s, GlobalSearchScope.allScope(project)).isEmpty())
                .collect(Collectors.toSet()
            );
        }
    }

    @Deprecated
    public static class CollectProjectUniqueKeysStrong implements Processor<String> {
        final Project project;

        final ID<String, ?>  id;

        final Set<String> stringSet;

        final Collection<String> strongKeys;

        @Deprecated
        public CollectProjectUniqueKeysStrong(Project project, ID<String, ?>  id, Collection<String> strongKeys) {
            this.project = project;
            this.id = id;
            this.strongKeys = strongKeys;
            this.stringSet = new HashSet<>();
        }

        @Override
        public boolean process(String s) {
            if(!strongKeys.contains(s)) {
                this.stringSet.add(s);
            }
            return true;
        }

        public Set<String> getResult() {
            return stringSet.stream()
                .filter(
                    s -> !FileBasedIndex.getInstance().getContainingFiles(id, s, GlobalSearchScope.allScope(project)).isEmpty()
                )
                .collect(Collectors.toSet()
            );
        }
    }

    public static Set<String> createResult(Project project, ID<String, ?> id) {
        return new HashSet<>(IndexUtil.getAllKeysForProject(id, project));
    }

    @Deprecated
    public static Set<String> createResult(Project project, ID<String, ?>  id, Collection<String> strongKeys) {
        CollectProjectUniqueKeysStrong collector = new CollectProjectUniqueKeysStrong(project, id, strongKeys);
        FileBasedIndex.getInstance().processAllKeys(id, collector, project);
        return collector.getResult();
    }
}
