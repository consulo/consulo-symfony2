package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after ServiceContainerGlobalVariableCollector")
public class FileDocVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        PsiFile containingFile = parameter.getContainingFile();
        if(!(containingFile instanceof TwigFile)) {
            return;
        }

        variables.putAll(convertHashMapToTypeSet(TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) containingFile)));
    }

    private static Map<String, Set<String>> convertHashMapToTypeSet(Map<String, String> hashMap) {
        HashMap<String, Set<String>> globalVars = new HashMap<>();

        for(final Map.Entry<String, String> entry: hashMap.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                globalVars.put(entry.getKey(), new HashSet<>(TwigTypeResolveUtil.splitTwigDocTypes(value)));
            } else {
                globalVars.put(entry.getKey(), new HashSet<>());
            }
        }

        return globalVars;
    }
}
