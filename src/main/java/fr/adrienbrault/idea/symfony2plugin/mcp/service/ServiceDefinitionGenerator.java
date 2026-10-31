package fr.adrienbrault.idea.symfony2plugin.mcp.service;

import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates Symfony service definitions for class name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class ServiceDefinitionGenerator {

    public static final class ServiceDefinitionModel {
        private final String myServiceName;
        private final List<MethodParameter.MethodModelParameter> myModelParameters;

        public ServiceDefinitionModel(String serviceName, List<MethodParameter.MethodModelParameter> modelParameters) {
            myServiceName = serviceName;
            myModelParameters = modelParameters;
        }

        public String getServiceName() {
            return myServiceName;
        }

        public List<MethodParameter.MethodModelParameter> getModelParameters() {
            return myModelParameters;
        }

        public String component1() {
            return myServiceName;
        }

        public List<MethodParameter.MethodModelParameter> component2() {
            return myModelParameters;
        }

        public ServiceDefinitionModel copy(String serviceName, List<MethodParameter.MethodModelParameter> modelParameters) {
            return new ServiceDefinitionModel(serviceName, modelParameters);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ServiceDefinitionModel)) {
                return false;
            }
            ServiceDefinitionModel that = (ServiceDefinitionModel) o;
            return Objects.equals(myServiceName, that.myServiceName) && Objects.equals(myModelParameters, that.myModelParameters);
        }

        @Override
        public int hashCode() {
            return myServiceName.hashCode() * 31 + myModelParameters.hashCode();
        }

        @Override
        public String toString() {
            return "ServiceDefinitionModel(serviceName=" + myServiceName + ", modelParameters=" + myModelParameters + ")";
        }
    }

    /**
     * Generates a service definition for the given class.
     * Validation should be handled by the caller (e.g., MCP tool).
     *
     * @param className The fully qualified class name (FQN) - should be validated by caller
     */
    public @Nullable String generate(Project project, String className, ServiceBuilder.OutputType outputType, boolean useClassNameAsId) {
        ServiceDefinitionModel model = createModel(project, className);
        if (model == null) {
            return null;
        }

        String normalizedClassName = StringUtils.stripStart(className, "\\");

        ServiceBuilder builder = new ServiceBuilder(model.getModelParameters(), project, useClassNameAsId);
        return builder.build(outputType, normalizedClassName, model.getServiceName());
    }

    public @Nullable ServiceDefinitionModel createModel(Project project, String className) {
        ServiceBuilder.ServiceDefinitionModel model = ServiceBuilder.createModel(project, className);
        return model != null ? toModel(model) : null;
    }

    private static ServiceDefinitionModel toModel(ServiceBuilder.ServiceDefinitionModel model) {
        return new ServiceDefinitionModel(model.getServiceName(), new ArrayList<>(model.getModelParameters()));
    }
}
