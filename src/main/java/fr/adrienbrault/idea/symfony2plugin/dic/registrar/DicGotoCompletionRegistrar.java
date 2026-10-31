package fr.adrienbrault.idea.symfony2plugin.dic.registrar;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.ParameterPercentWrapInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DicGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // getParameter('FOO')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern()), psiElement -> {

                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "hasParameter")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "getParameter")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ParametersConfigurator", "set")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "get")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "set")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "has")
                    .match();

                if(methodMatchParameter == null) {
                    MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter((StringLiteralExpression) context);
                    if (methodReference == null || !ServiceContainerUtil.isContainerBagParameterAccess(methodReference)) {
                        return null;
                    }
                }

                if(methodMatchParameter == null && PsiElementUtils.getParameterIndexValue(context) != 0) {
                    return null;
                }

                return new ParameterContributor((StringLiteralExpression) context);
            }
        );

        // param('<caret>')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getFunctionWithFirstStringPattern("param")), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                return new ParameterContributor((StringLiteralExpression) context);
            }
        );

        // env('<caret>')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getFunctionWithFirstStringPattern("env")), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                return new EnvironmentVariableContributor((StringLiteralExpression) context);
            }
        );

        // #[Autowire('%kernel.project_dir%')]
        // #[Autowire(value: '%kernel.project_dir%')]
        registrar.register(
            PlatformPatterns.or(
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "value")
            ), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new ParameterWithPercentWrapContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        registrar.register(
            PlatformPatterns.or(
                // #[TaggedIterator('app.handler')] iterable $handlers
                // #[TaggedIterator(tag: 'app.handler')] iterable $handlers
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.TAGGED_ITERATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.TAGGED_ITERATOR_ATTRIBUTE_CLASS, "tag"),

                // #[TaggedLocator('app.handler')] ContainerInterface $handlers
                // #[TaggedLocator(tag: 'app.handler')] ContainerInterface $handlers
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.TAGGED_LOCATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.TAGGED_LOCATOR_ATTRIBUTE_CLASS, "tag"),

                // #[Autoconfigure(['app.some_tag'])]
                // #[Autoconfigure(tags: ['app.some_tag'])]
                PhpElementsUtil.getFirstAttributeArrayStringPattern(ServiceContainerUtil.AUTOCONFIGURE_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentArrayStringPattern(ServiceContainerUtil.AUTOCONFIGURE_ATTRIBUTE_CLASS, "tags"),

                // #[AutoconfigureTag('app.handler')]
                // #[AutoconfigureTag(name: 'app.handler')]
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOCONFIGURE_TAG_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOCONFIGURE_TAG_ATTRIBUTE_CLASS, "name")
            ), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new TaggedIteratorContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[Autowire(param: 'kernel.debug')]
        registrar.register(
            PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "param"),
            psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new ParameterContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[Autowire(env: 'SOME_ENV_VAR')]
        registrar.register(
            PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "env"),
            psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new EnvironmentVariableContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[Autowire(service: 'some_service')]
        // #[AsDecorator(decorates: 'some_service')]
        // #[AutowireServiceClosure('some_service')]
        // #[AutowireServiceClosure(service: 'some_service')]
        // #[AutowireMethodOf('some_service')]
        // #[AutowireMethodOf(service: 'some_service')]
        // #[AutowireCallable(service: 'some_service')]
        registrar.register(
            PlatformPatterns.or(
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service"),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.DECORATOR_ATTRIBUTE_CLASS, "decorates"),
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.DECORATOR_ATTRIBUTE_CLASS),

                // #[AutowireLocator(['app.some_tag', 'app.some_tag'])]
                // #[AutowireLocator(services: ['app.some_tag'])]
                PhpElementsUtil.getFirstAttributeArrayStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentArrayStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS, "services"),

                // #[AutowireLocator('app.some_tag'])]
                // #[AutowireLocator(services: 'app.some_tag')]
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS, "services"),

                // #[AutowireLocator(exclude: ['app.some_tag'])]
                // #[AutowireLocator(exclude: 'app.some_tag')]
                PhpElementsUtil.getAttributeNamedArgumentArrayStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS, "exclude"),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS, "exclude"),

                // #[AutowireServiceClosure('some_service')]
                // #[AutowireServiceClosure(service: 'some_service')]
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOWIRE_SERVICE_CLOSURE_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_SERVICE_CLOSURE_ATTRIBUTE_CLASS, "service"),

                // #[AutowireMethodOf('some_service')]
                // #[AutowireMethodOf(service: 'some_service')]
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOWIRE_METHOD_OF_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_METHOD_OF_ATTRIBUTE_CLASS, "service"),

                // #[AutowireCallable(service: 'some_service')]
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_CALLABLE_ATTRIBUTE_CLASS, "service")
            ),
            psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new ServiceContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[AutowireCallable(service: 'some_service', method: 'methodName')]
        registrar.register(
            PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_CALLABLE_ATTRIBUTE_CLASS, "method"),
            psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new AutowireCallableMethodContributor((StringLiteralExpression) context, phpAttribute);
                }

                return null;
            }
        );

        // #[When('dev')]
        registrar.register(
            PlatformPatterns.or(
                PhpElementsUtil.getFirstAttributeStringPattern("\\Symfony\\Component\\DependencyInjection\\Attribute\\When")
            ), psiElement -> new GotoCompletionProvider(psiElement) {
                @Override
                public Collection<LookupElement> getLookupElements() {
                    return Arrays.stream((new String[]{"prod", "dev", "test", "never"}))
                        .map((Function<String, LookupElement>) s -> LookupElementBuilder.create(s).withIcon(Symfony2Icons.SYMFONY))
                        .collect(Collectors.toList());
                }
            }
        );
    }

    private static class ParameterContributor extends GotoCompletionProvider {

        public ParameterContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            for (Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getProject()).entrySet()) {
                results.add(new ParameterLookupElement(entry.getValue()));
            }

            return results;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            return ServiceUtil.getParameterDefinition(getProject(), contents);
        }
    }

    private static class TaggedIteratorContributor extends GotoCompletionProvider {
        public TaggedIteratorContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            return TagNameCompletionProvider.getTagLookupElements(getProject());
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(ServiceUtil.getTaggedClasses(getProject(), contents));
        }
    }

    private static class ServiceContributor extends GotoCompletionProvider {
        public ServiceContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            return ServiceCompletionProvider.getLookupElements(this.getElement(), ContainerCollectionResolver.getServices(getProject()).values()).getLookupElements();
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(getProject(), contents);
            if (phpClass == null) {
                return Collections.emptyList();
            }

            return List.of(phpClass);
        }
    }

    private static class EnvironmentVariableContributor extends GotoCompletionProvider {
        public EnvironmentVariableContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            for (String envVar : DotEnvUtil.getEnvironmentVariables(getProject())) {
                results.add(LookupElementBuilder.create(envVar).withIcon(Symfony2Icons.SYMFONY));
            }

            return results;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            // Strip processor prefix if present: bool:SOME_ENV_VAR -> SOME_ENV_VAR
            // Supports: int:, bool:, string:, json:, resolve:, etc.
            Matcher matcher = Pattern.compile("^[\\w-_^:]+:(.*)$", Pattern.MULTILINE).matcher(contents);
            if (matcher.find()) {
                contents = matcher.group(1);
            }

            return DotEnvUtil.getEnvironmentVariableTargets(getProject(), contents);
        }
    }

    private static class ParameterWithPercentWrapContributor extends GotoCompletionProvider {
        public ParameterWithPercentWrapContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            for (Map.Entry<String, ContainerParameter> entry : ContainerCollectionResolver.getParameters(getProject()).entrySet()) {
                results.add(new ParameterLookupElement(
                    entry.getValue(),
                    ParameterPercentWrapInsertHandler.getInstance(),
                    getElement().getText()
                ));
            }

            return results;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            // Strip % characters if present: %kernel.debug% -> kernel.debug
            contents = StringUtils.strip(contents, "%");

            return ServiceUtil.getParameterDefinition(getProject(), contents);
        }
    }

    private static class AutowireCallableMethodContributor extends GotoCompletionProvider {
        private final PhpAttribute phpAttribute;

        public AutowireCallableMethodContributor(StringLiteralExpression element, PhpAttribute phpAttribute) {
            super(element);
            this.phpAttribute = phpAttribute;
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            PhpClass phpClass = getServiceClass();
            if (phpClass == null) {
                return results;
            }

            for (Method method : phpClass.getMethods()) {
                // Only public non-static methods
                if (method.getAccess().isPublic() && !method.isStatic()) {
                    results.add(LookupElementBuilder.create(method.getName())
                        .withIcon(IconDescriptorUpdaters.getIcon(method, 0))
                        .withTypeText(phpClass.getName(), true));
                }
            }

            return results;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String methodName = GotoCompletionUtil.getStringLiteralValue(element);
            if (methodName == null) {
                return Collections.emptyList();
            }

            PhpClass phpClass = getServiceClass();
            if (phpClass == null) {
                return Collections.emptyList();
            }

            Method method = phpClass.findMethodByName(methodName);
            if (method != null && method.getAccess().isPublic() && !method.isStatic()) {
                return List.of(method);
            }

            return Collections.emptyList();
        }

        private PhpClass getServiceClass() {
            // Find the 'service' argument in the same attribute using the utility method
            String serviceId = PhpElementsUtil.getAttributeArgumentStringByName(phpAttribute, "service");
            if (serviceId != null && !serviceId.isEmpty()) {
                return ServiceUtil.getResolvedClassDefinition(getProject(), serviceId);
            }

            return null;
        }
    }
}
