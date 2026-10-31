package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.project.Project;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandUtil {

    public static Map<String, String> getCommandHelper(Project project) {

        Map<String, String> map = new HashMap<>();
        for(PhpClass phpClass: PhpIndexUtil.getAllSubclasses(project, "\\Symfony\\Component\\Console\\Helper\\HelperInterface")) {
            String helperName = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");
            if(helperName != null) {
                map.put(helperName, phpClass.getPresentableFQN());
            }
        }

        return map;
    }
}
