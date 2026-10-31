package fr.adrienbrault.idea.symfony2plugin.ux.variable.collector;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Support variables piped from "AsTwigComponent, AsLiveComponent" to Twig templates
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(id = "SymfonyUxComponentVariableCollector", order = "after GlobalTwigConfigVariableCollector")
public class UxComponentVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {
        Project project = parameter.getProject();
        Collection<PhpClass> componentClassesForTemplateFile = UxUtil.getComponentClassesForTemplateFile(project, parameter.getContainingFile());

        if (!componentClassesForTemplateFile.isEmpty()) {
            variables.put("attributes", new PsiVariable("\\Symfony\\UX\\TwigComponent\\ComponentAttributes"));
        }

        for (PhpClass phpClass : componentClassesForTemplateFile) {
            variables.put("this", new PsiVariable(phpClass.getFQN()));

            for (Field field : phpClass.getFields()) {
                if (field.getModifier().isPublic()) {
                    variables.put(field.getName(), new PsiVariable(
                        PhpIndex.getInstance(project).completeType(project, field.getType(), new HashSet<>()).getTypes()
                    ));
                }
            }
        }
    }
}
