package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.psi.ResolveResult;
import consulo.language.util.ProcessingContext;
import consulo.project.DumbService;
import consulo.util.collection.ContainerUtil;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlText;
import consulo.xml.language.psi.XmlToken;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ClassPublicMethodReference;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.config.xml.provider.ServiceReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {

        // <argument type="service" id="service_container" />
        // <service alias="Foobar" />
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, PlatformPatterns.or(XmlHelper.getArgumentServiceIdPattern(), XmlHelper.getServiceAliasPattern())),
            new ServiceReferenceProvider()
        );

        // <factory service="factory_service" />
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getFactoryServiceCompletionPattern()),
            new ServiceReferenceProvider()
        );

        // <autowiring-type>Acme\TransformerInterface</autowiring-type>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getAutowiringTypePattern()),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    String value = getXmlTextValue(psiElement);
                    if (value == null) {
                        return new PsiReference[0];
                    }
                    return new PsiReference[]{ new PhpClassReference(psiElement, value) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // <service class="%foo.class%">
        // <service class="Class\Name">
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getServiceClassAttributeWithIdPattern()),
            new ClassPsiReferenceProvider()
        );

        // Symfony 3.3 shortcut
        // <service id="Class\Name">
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getServiceIdAttributePattern()),
            new ClassAsIdPsiReferenceProvider()
        );

        // <parameter key="fos_user.user_manager.class">FOS\UserBundle\Doctrine\UserManager</parameter>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getParameterClassValuePattern()),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {

                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    // get the service name "service_container"
                    String text = psiElement.getText();
                    return new PsiReference[]{ new PhpClassReference(psiElement, text) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // <argument>%form.resolved_type_factory.class%</argument>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getArgumentValuePattern()),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    String value = getXmlTextValue(psiElement);
                    if (value == null || StringUtils.isBlank(value)) {
                        return new PsiReference[0];
                    }
                    return new PsiReference[]{ new ParameterXmlReference(psiElement, value) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // <argument type="constant">Foobar\Foo</argument>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getArgumentValueWithTypePattern("constant")),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    String value = getXmlTextValue(psiElement);
                    if (value == null || StringUtils.isBlank(value)) {
                        return new PsiReference[0];
                    }
                    return new PsiReference[]{ new ConstantXmlReference(psiElement, value) };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // <tag name="kernel.event_subscriber" />
        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("tag", "name")
                    .inside(XmlHelper.getInsideTagPattern("services"))
                    .inFile(XmlHelper.getXmlFilePattern())
            ),

            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {

                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    if(element instanceof XmlAttributeValue) {
                        return new PsiReference[] { new TagReference(element, PsiElementUtils.trimQuote(element.getText()))};
                    }

                    return new PsiReference[0];
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );


        // <tag event="foo" method="kernel.event_subscriber" />
        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("tag", "method")
                    .inside(XmlHelper.getInsideTagPattern("services"))
                    .inFile(XmlHelper.getXmlFilePattern())
            ),
            new ClassMethodReferenceProvider()
        );

        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("call", "method")
                    .inside(XmlHelper.getInsideTagPattern("services"))
                    .inFile(XmlHelper.getXmlFilePattern())
            ),
            new ClassMethodReferenceProvider()
        );

        // <factory class="AppBundle\Trivago\ConfigFactory"/>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("factory", "class")
                    .inFile(XmlHelper.getXmlFilePattern())
            ),
            new ClassPsiReferenceProvider()
        );

        // <factory class="AppBundle\Trivago\ConfigFactory" method="create"/>
        // <factory service="foo" method="create"/>
        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("factory", "method")
                    .inside(XmlHelper.getInsideTagPattern("services"))
                    .inFile(XmlHelper.getXmlFilePattern())
            ),
            new ChainPsiReferenceProvider(
                new FactoryClassMethodPsiReferenceProvider(),
                new FactoryServiceMethodPsiReferenceProvider()
            )
        );

        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getParameterWithClassEndingPattern()
                    .inside(XmlHelper.getInsideTagPattern("parameters"))
                    .inFile(XmlHelper.getXmlFilePattern())
            ),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    if(element instanceof XmlToken) {
                        return new PsiReference[] {
                            new PhpClassReference(element, PsiElementUtils.removeIdeaRuleHack(PsiElementUtils.trimQuote(element.getText())), true)
                        };
                    }

                    return new PsiReference[0];
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        registrar.registerReferenceProvider(
            PlatformPatterns.and(
                XmlHelper.XML_EXTENSION,
                XmlHelper.getTagAttributePattern("tag", "event").inside(XmlHelper.getInsideTagPattern("services"))
            ),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {

                    if(!Symfony2ProjectComponent.isEnabled(element)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new EventDispatcherEventReference(element, PsiElementUtils.removeIdeaRuleHack(PsiElementUtils.trimQuote(element.getText())))
                    };

                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    /**
     * Returns the text value of the XmlText parent of {@code psiElement}, or {@code null} if the plugin is
     * disabled or the parent is not an {@link XmlText}.
     */
    @Nullable
    private static String getXmlTextValue(PsiElement psiElement) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }
        PsiElement parent = psiElement.getParent();
        if (!(parent instanceof XmlText)) {
            return null;
        }
        return ((XmlText) parent).getValue();
    }

    /**
     * Builds a {@link ClassMethodStringPsiReference} for {@code method} on {@code phpClass}, or returns
     * an empty array if either is {@code null}/missing.
     */
    private static PsiReference[] buildMethodStringReference(PsiElement psiElement, @Nullable PhpClass phpClass, String method) {
        if (phpClass == null) {
            return new PsiReference[0];
        }
        Method targetMethod = phpClass.findMethodByName(method);
        if (targetMethod == null) {
            return new PsiReference[0];
        }
        return new PsiReference[]{
            new ClassMethodStringPsiReference(psiElement, phpClass.getFQN(), targetMethod.getName()),
        };
    }

    private static class ClassPsiReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            return new PsiReference[] {
                new ServiceIdReference((XmlAttributeValue) psiElement)
            };
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    /**
     * Shortcut for service tag without class attribute
     *
     * <service id="Foobar\Foobar"/>
     */
    private static class ClassAsIdPsiReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            PsiElement parent = psiElement.getParent();
            if(!(parent instanceof XmlAttribute) && YamlHelper.isClassServiceId(parent.getText())) {
                return new PsiReference[0];
            }

            // invalidate on class attribute
            PsiElement xmlTag = parent.getParent();
            if(!(xmlTag instanceof XmlTag) || ((XmlTag) xmlTag).getAttribute("class") != null) {
                return new PsiReference[0];
            }

            return new PsiReference[] {
                new ServiceIdWithoutParameterReference((XmlAttributeValue) psiElement)
            };
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    /**
     * <factory service="foo" method="create"/>
     */
    private static class FactoryServiceMethodPsiReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext context) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            String method = ((XmlAttributeValue) psiElement).getValue();
            if(StringUtils.isBlank(method)) {
                return new PsiReference[0];
            }

            return buildMethodStringReference(psiElement, XmlHelper.getPhpClassForServiceFactory((XmlAttributeValue) psiElement), method);
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    private static class ClassMethodReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext context) {

            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return new PsiReference[0];
            }

            // check for valid xml file and services container
            if(!XmlPatterns.psiElement().inside(XmlHelper.getInsideTagPattern("services")).inFile(XmlHelper.getXmlFilePattern()).accepts(psiElement)) {
                return new PsiReference[0];
            }

            String serviceDefinitionClass = XmlHelper.getServiceDefinitionClass(psiElement);
            if(serviceDefinitionClass == null) {
                return new PsiReference[0];
            }

            return new PsiReference[] { new ClassPublicMethodReference(psiElement, serviceDefinitionClass)};
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    private static class ServiceIdReference extends PsiPolyVariantReferenceBase<PsiElement> {

        private final XmlAttributeValue psiElement;

        public ServiceIdReference(XmlAttributeValue psiElement) {
            super(psiElement);
            this.psiElement = psiElement;
        }

        @Override
        public ResolveResult[] multiResolve(boolean b) {
            if (DumbService.isDumb(getElement().getProject())) {
                return ResolveResult.EMPTY_ARRAY;
            }

            String value = this.psiElement.getValue();


            Collection<PsiElement> serviceClassTargets = ServiceUtil.getServiceClassTargets(getElement().getProject(), value);

            // @TODO: on implement multiple service resolve; we can make it nicer here
            // self add on compiler parameter, in this case we dont have a target;
            // to not get ide warnings
            if(serviceClassTargets.isEmpty() && value.startsWith("%") && value.endsWith("%") && value.length() > 2 && ContainerCollectionResolver.getParameterNames(getElement().getProject()).contains(StringUtils.strip(value, "%"))) {
                serviceClassTargets.add(getElement());
            }

            return PsiElementResolveResult.createResults(serviceClassTargets);
        }

        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

    }

    /**
     * <service id="Foobar\Foo"/>
     */
    private static class ServiceIdWithoutParameterReference extends PsiPolyVariantReferenceBase<PsiElement> {

        private final XmlAttributeValue psiElement;

        private ServiceIdWithoutParameterReference(XmlAttributeValue psiElement) {
            super(psiElement);
            this.psiElement = psiElement;
        }

        @Override
        public ResolveResult[] multiResolve(boolean b) {
            if (DumbService.isDumb(getElement().getProject())) {
                return ResolveResult.EMPTY_ARRAY;
            }

            String value = this.psiElement.getValue();
            return PsiElementResolveResult.createResults(ServiceUtil.getServiceClassTargets(getElement().getProject(), value));
        }

        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }

    /**
     * <factory class="FooBar" method="cre<caret>ate"/>
     */
    private static class FactoryClassMethodPsiReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
            if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof XmlAttributeValue)) {
                return new PsiReference[0];
            }

            String method = ((XmlAttributeValue) psiElement).getValue();
            if(StringUtils.isBlank(method)) {
                return new PsiReference[0];
            }

            return buildMethodStringReference(psiElement, XmlHelper.getPhpClassForClassFactory((XmlAttributeValue) psiElement), method);
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    private static class ChainPsiReferenceProvider extends PsiReferenceProvider {
        private final PsiReferenceProvider[] psiReferenceProviders;

        ChainPsiReferenceProvider(PsiReferenceProvider... psiReferenceProviders) {
            this.psiReferenceProviders = psiReferenceProviders;
        }

        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
            Collection<PsiReference> psiReferences = new ArrayList<>();

            for (PsiReferenceProvider provider : this.psiReferenceProviders) {
                ContainerUtil.addAll(psiReferences, provider.getReferencesByElement(psiElement, processingContext));
            }

            return psiReferences.toArray(new PsiReference[0]);
        }

        public boolean acceptsTarget(PsiElement target) {
            return Symfony2ProjectComponent.isEnabled(target);
        }
    }

    private static class ClassMethodStringPsiReference extends PsiPolyVariantReferenceBase<PsiElement> {
        private final String aClass;

        private final String method;

        ClassMethodStringPsiReference(PsiElement psiElement, String aClass, String method) {
            super(psiElement);
            this.aClass = aClass;
            this.method = method;
        }

        @Override
        public ResolveResult[] multiResolve(boolean b) {
            if (DumbService.isDumb(getElement().getProject())) {
                return ResolveResult.EMPTY_ARRAY;
            }

            Method classMethod = PhpElementsUtil.getClassMethod(getElement().getProject(), aClass, method);
            if(classMethod == null) {
                return new ResolveResult[0];
            }

            return PsiElementResolveResult.createResults(classMethod);
        }

        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }
}