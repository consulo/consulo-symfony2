package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.util.function.Supplier;

/**
 * consulo-port: the registered inspections were nested classes upstream; they are now top-level classes in this package:
 * {@link ServiceDeprecatedClassesInspectionYaml}, {@link ServiceDeprecatedClassesInspectionXml}, {@link ServiceDeprecatedClassesInspectionPhp}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class ServiceDeprecatedClassesInspection {
    static final class ProblemRegistrar {
        private ProblemRegistrar() {
        }

        static void attachDeprecatedProblem(
            PsiElement element,
            String text,
            ProblemsHolder holder,
            Supplier<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector
        ) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), text, lazyServiceCollector.get());
            if (phpClass == null) {
                return;
            }

            PhpDocComment docComment = phpClass.getDocComment();
            if (docComment != null && docComment.getTagElementsByName("@deprecated").length > 0) {
                holder.registerProblem(
                    element,
                    "Class '" + phpClass.getName() + "' is deprecated",
                    ProblemHighlightType.LIKE_DEPRECATED
                );
            }
        }

        static void attachServiceDeprecatedProblem(
            PsiElement element,
            String serviceName,
            ProblemsHolder holder,
            Supplier<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector
        ) {
            ContainerService containerService = lazyServiceCollector.get().getCollector().getServices().get(serviceName);
            if (containerService == null || !containerService.isDeprecated()) {
                return;
            }

            holder.registerProblem(
                element,
                "Service '" + serviceName + "' is deprecated",
                ProblemHighlightType.LIKE_DEPRECATED
            );
        }
    }
}
