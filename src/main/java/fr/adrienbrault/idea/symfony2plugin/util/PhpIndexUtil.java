package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpInterfaceFqnIndex;
import com.jetbrains.php.util.PhpContractUtil;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpIndexUtil {
    /**
     * Collect PhpClass which are inside current namespace and in sub-namespaces
     *
     * @param project current project
     * @param namespaceName namespace name should start with \ and end with "\"
     * @return classes inside namespace and sub-namespace
     */
    public static Collection<PhpClass> getPhpClassInsideNamespace(Project project, String namespaceName) {
        return getPhpClassInsideNamespace(project, PhpIndex.getInstance(project), namespaceName, false);
    }

    /**
     * Collect PhpClass which are inside current namespace and in sub-namespaces
     *
     * @param project current project
     * @param namespaceName namespace name should start with \ and end with "\"
     * @return classes inside namespace and sub-namespace
     */
    public static Collection<PhpClass> getPhpClassInsideNamespace(Project project, String namespaceName, boolean excludeInterfaces) {
        return getPhpClassInsideNamespace(project, PhpIndex.getInstance(project), namespaceName, excludeInterfaces);
    }

    public static Collection<PhpClass> getAllSubclasses(Project project, String clazz) {
        Collection<PhpClass> phpClasses = new ArrayList<>();

        PhpIndex.getInstance(project).processAllSubclasses(clazz, phpClass -> {
            phpClasses.add(phpClass);
            return true;
        });

        return phpClasses;
    }


    private static Collection<PhpClass> getPhpClassInsideNamespace(Project project, PhpIndex phpIndex, String namespaceName, boolean excludeInterfaces) {
        PhpContractUtil.assertFqn(namespaceName);
        Set<String> classes = new HashSet<>();

        FileBasedIndex.getInstance().processAllKeys(PhpClassFqnIndex.KEY, fqn -> {
            if (fqn.startsWith(namespaceName)) {
                classes.add(fqn);
            }
            return true;
        }, project);

        if (!excludeInterfaces) {
            FileBasedIndex.getInstance().processAllKeys(PhpInterfaceFqnIndex.KEY, fqn -> {
                if (fqn.startsWith(namespaceName)) {
                    classes.add(fqn);
                }
                return true;
            }, project);
        }

        Collection<PhpClass> clazzes = new HashSet<>();
        for (String s : classes) {
            clazzes.addAll(phpIndex.getAnyByFQN(s));
        }

        return clazzes;
    }

    public static boolean hasNamespace(Project project, String namespaceName) {

        if(!namespaceName.startsWith("\\")) {
            namespaceName = "\\" + namespaceName;
        }

        return !PhpIndex.getInstance(project).getChildNamespacesByParentName(namespaceName + "\\").isEmpty();
    }
}
