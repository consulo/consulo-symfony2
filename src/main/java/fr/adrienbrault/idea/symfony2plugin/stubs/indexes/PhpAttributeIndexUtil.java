package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class for querying PhpAttributeIndex
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpAttributeIndexUtil {
    /**
     * Get all indexed data for the given attribute FQN
     *
     * @param project      The project
     * @param attributeFqn The attribute FQN (e.g., "\Twig\Attribute\AsTwigFilter")
     * @return scoped targets for the given attribute FQN
     */
    public static Collection<PhpAttributeIndex.AttributeTarget> getAttributeData(Project project, String attributeFqn) {
        return FileBasedIndex.getInstance()
            .getValues(
                PhpAttributeIndex.KEY,
                attributeFqn,
                GlobalSearchScope.allScope(project)
            )
            .stream()
            .flatMap(Collection::stream)
            .toList();
    }

    public static Collection<PhpAttributeIndex.AttributeTarget> getTargetsWithAttributeScope(
        Project project,
        String attributeFqn,
        PhpAttributeIndex.TargetScope scope
    ) {
        return getAttributeData(project, attributeFqn)
            .stream()
            .filter(target -> target.scope() == scope)
            .toList();
    }

    public static Collection<PhpAttributeIndex.AttributeTarget> getMethodTargetsWithAttribute(Project project, String attributeFqn) {
        return getTargetsWithAttributeScope(project, attributeFqn, PhpAttributeIndex.TargetScope.METHOD);
    }

    /**
     * Get all PHP classes that have the given attribute
     *
     * @param project      The project
     * @param attributeFqn The attribute FQN
     * @return Collection of PhpClass instances
     */
    public static Collection<PhpClass> getClassesWithAttribute(Project project, String attributeFqn) {
        Collection<PhpClass> classes = new ArrayList<>();

        for (PhpAttributeIndex.AttributeTarget target : getTargetsWithAttributeScope(project, attributeFqn, PhpAttributeIndex.TargetScope.PHP_CLASS)) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, normalizeFqn(target.classFqn()));
            if (phpClass != null) {
                classes.add(phpClass);
            }
        }

        return classes;
    }

    private static String normalizeFqn(String fqn) {
        return fqn.startsWith("\\") ? fqn : "\\" + fqn;
    }
}
