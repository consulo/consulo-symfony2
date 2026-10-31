package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.project.Project;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.ParameterVisitor;

import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterResolverConsumer implements Consumer<ParameterVisitor> {
    private final Project project;

    private final Consumer<Parameter> consumer;

    public ParameterResolverConsumer(Project project, Consumer<Parameter> consumer) {
        this.project = project;
        this.consumer = consumer;
    }

    @Override
    public void accept(ParameterVisitor parameter) {
        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(
            this.project,
            parameter.getClassName(),
            new ContainerCollectionResolver.LazyServiceCollector(this.project)
        );

        if(serviceClass == null) {
            return;
        }

        Method method = serviceClass.findMethodByName(parameter.getMethod());
        if (method == null) {
            return;
        }

        Parameter[] methodParameter = method.getParameters();
        if(parameter.getParameterIndex() >= methodParameter.length) {
            return;
        }

        consumer.accept(methodParameter[parameter.getParameterIndex()]);
    }
}