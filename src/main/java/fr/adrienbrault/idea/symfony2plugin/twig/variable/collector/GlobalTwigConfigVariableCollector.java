package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * config.yml:
 *  twig:
 *      globals:
 *          foobar: "@foobar"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after MarcoScopeVariableCollector")
public class GlobalTwigConfigVariableCollector implements TwigFileVariableCollector {

    private static final Key<CachedValue<Map<String, PsiVariable>>> CACHE = new Key<>("TWIG_CONFIGURATION_GLOBALS");

    @Override
    public void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {
        variables.putAll(getGlobals(parameter.getProject()));
    }

    private Map<String, PsiVariable> getGlobals(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(
                getGlobalsInner(project),
                FileIndexCaches.getModificationTrackerForIndexId(project, ConfigStubIndex.KEY),
                FileIndexCaches.getModificationTrackerForIndexId(project, ServicesDefinitionStubIndex.KEY)
            ),
            false
        );
    }

    private static Map<String, PsiVariable> getGlobalsInner(Project project) {
        Map<String, PsiVariable> variableMap = new HashMap<>();

        for (VirtualFile virtualFile : ConfigUtil.getConfigurations(project, "twig")) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(!(file instanceof YAMLFile)) {
                continue;
            }

            for (Map.Entry<String, String> entry : TwigUtil.getTwigGlobalsFromYamlConfig((YAMLFile) file).entrySet()) {
                String value = entry.getValue();

                String serviceClass = ContainerCollectionResolver.resolveService(
                    project,
                    YamlHelper.trimSpecialSyntaxServiceName(value)
                );

                if(serviceClass != null) {
                    variableMap.put(entry.getKey(), new PsiVariable("\\" + serviceClass));
                } else {
                    variableMap.put(entry.getKey(), new PsiVariable(new HashSet<>()));
                }
            }
        }

        return variableMap;
    }
}
