package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after FileDocVariableCollector")
public class ControllerVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        variables.putAll(TwigUtil.collectControllerTemplateVariables((TwigFile) psiFile));
    }
}
