package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after ControllerDocVariableCollector")
public class ServiceContainerGlobalVariableCollector implements TwigFileVariableCollector {
    private static final Key<CachedValue<Map<String, Set<String>>>> CACHE = new Key<>("TWIG_SERVICE_CONTAINER_GLOBALS");

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        variables.putAll(getGlobals(parameter.getProject()));
    }

    private static Map<String, Set<String>> getGlobals(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(
                getGlobalsInner(project),
                FileIndexCaches.getModificationTrackerForIndexId(project, ServicesDefinitionStubIndex.KEY),
                FileIndexCaches.getModificationTrackerForIndexId(project, PhpClassFqnIndex.KEY),
                SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project).getModificationTracker(SymfonyVarDirectoryWatcher.Scope.CONTAINER)
            ),
            false
        );
    }

    private static Map<String, Set<String>> getGlobalsInner(Project project) {
        Map<String, Set<String>> map = new HashMap<>();

        TwigGlobalsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigGlobalsServiceParser.class);
        for(Map.Entry<String, TwigGlobalVariable> globalVariableEntry: twigPathServiceParser.getTwigGlobals().entrySet()) {
            if(globalVariableEntry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.SERVICE) {
                String serviceName = globalVariableEntry.getValue().getValue();
                PhpClass phpClass = ServiceUtil.getServiceClass(project, serviceName);
                if(phpClass != null) {
                    String key = globalVariableEntry.getKey();
                    map.putIfAbsent(key, new HashSet<>());
                    map.get(key).add("\\" + StringUtils.stripStart(phpClass.getFQN(), "\\"));
                }
            }
        }

        return toImmutableMap(map);
    }

    private static Map<String, Set<String>> toImmutableMap(Map<String, Set<String>> map) {
        Map<String, Set<String>> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key, Collections.unmodifiableSet(value)));

        return Collections.unmodifiableMap(result);
    }
}
