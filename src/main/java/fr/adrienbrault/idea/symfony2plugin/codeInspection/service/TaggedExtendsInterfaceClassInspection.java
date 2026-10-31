package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * consulo-port: the registered inspections were nested classes upstream; they are now top-level classes in this package:
 * {@link TaggedExtendsInterfaceClassInspectionYaml}, {@link TaggedExtendsInterfaceClassInspectionXml}.
 * The upstream file-private helper functions live here as package-private static methods.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TaggedExtendsInterfaceClassInspection {
    static void registerTaggedProblems(
        PsiElement source,
        Set<String> tags,
        String serviceClass,
        ProblemsHolder holder,
        Supplier<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector
    ) {
        if (tags.isEmpty()) {
            return;
        }

        PhpClass phpClass = null;

        for (String tag : tags) {
            String missingTagInstance = null;

            for (String expectedClass : ServiceUtil.TAG_INTERFACES.getOrDefault(tag, new String[0])) {
                // load PhpClass only if we need it, on error exit
                PhpClass resolvedClass = phpClass;
                if (resolvedClass == null) {
                    resolvedClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceClass, lazyServiceCollector.get());
                    if (resolvedClass == null) {
                        return;
                    }
                }
                phpClass = resolvedClass;

                // skip unknown classes
                if (PhpElementsUtil.getClassesInterface(resolvedClass.getProject(), expectedClass).isEmpty()) {
                    continue;
                }

                // check interfaces
                if (!PhpElementsUtil.isInstanceOf(resolvedClass, expectedClass)) {
                    missingTagInstance = expectedClass;
                    continue;
                }

                missingTagInstance = null;
                break;
            }

            // check interfaces
            if (missingTagInstance != null) {
                holder.registerProblem(source, "Class needs to implement '" + StringUtils.stripStart(missingTagInstance, "\\") + "' for tag '" + tag + "'", ProblemHighlightType.WEAK_WARNING);
            }
        }
    }

    /**
     * <service class="Foo\\Bar" id="required_attribute">
     * <service id="Foo\\Bar" />
     */
    static @Nullable String getClassNameFromServiceDefinition(
        PsiElement element,
        ElementPattern<?> xmlServiceClassAttrPattern,
        ElementPattern<?> xmlServiceIdAttrPattern
    ) {
        if (xmlServiceClassAttrPattern.accepts(element)) {
            // <service class="Foo\\Bar" id="required_attribute">
            String text = PsiElementUtils.trimQuote(element.getText());
            return StringUtils.isNotBlank(text) ? text : null;
        }
        else if (xmlServiceIdAttrPattern.accepts(element)) {
            // <service id="Foo\\Bar" />
            String text = PsiElementUtils.trimQuote(element.getText());
            return StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\") ? text : null;
        }
        else {
            return null;
        }
    }
}
