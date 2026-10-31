package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector;

import com.jetbrains.php.lang.psi.elements.MethodReference;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderRepositoryDetectorParameter {

    private final Project project;
    private final Collection<MethodReference> methodReferences;

    public QueryBuilderRepositoryDetectorParameter(Project project, Collection<MethodReference> methodReferences) {
        this.project = project;
        this.methodReferences = methodReferences;
    }

    @Nullable
    public MethodReference getMethodReferenceByName(String methodName) {

        for (MethodReference methodReference : methodReferences) {
            if(methodName.equals(methodReference.getName())) {
                return methodReference;
            }
        }

        return null;
    }

    public Project getProject() {
        return project;
    }

    public Collection<MethodReference> getMethodReferences() {
        return methodReferences;
    }

}
