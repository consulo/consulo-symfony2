package fr.adrienbrault.idea.symfony2plugin.config;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.NotNullLazyValue;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerServiceMetadata;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.translation.ConstraintMessageGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> results) {
        // we need project element; so get it from first item
        if(psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.getFirst().getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        var methodReturnPattern = PhpElementsUtil.getMethodReturnPattern();
        var classNamePattern = PhpElementsUtil.getClassNamePattern();
        var classMethodNamePattern = PhpElementsUtil.getClassMethodNamePattern();
        var constraintMessagePattern = ConstraintMessageGotoCompletionRegistrar.getConstraintPropertyMessagePattern();
        Supplier<ContainerCollectionResolver.ServiceCollector> serviceCollectorSupplier = null;

        for(PsiElement psiElement: psiElements) {
            if(methodReturnPattern.accepts(psiElement)) {
                this.formNameMarker(psiElement, results);
            }

            if(classNamePattern.accepts(psiElement)) {
                if (serviceCollectorSupplier == null) {
                    serviceCollectorSupplier = new LazyServiceCollector(project);
                }

                this.classNameMarker(project, psiElement, results, serviceCollectorSupplier);
                this.entityClassMarker(project, psiElement, results);
                this.repositoryClassMarker(project, psiElement, results);
                this.validatorClassMarker(project, psiElement, results);
                this.constraintValidatorClassMarker(project, psiElement, results);
            }

            if(classMethodNamePattern.accepts(psiElement)) {
                if (serviceCollectorSupplier == null) {
                    serviceCollectorSupplier = new LazyServiceCollector(project);
                }

                this.autowireConstructorMarker(project, psiElement, results, serviceCollectorSupplier);
            }

            // public $message = 'This value should not be blank.';
            if (constraintMessagePattern.accepts(psiElement)) {
                this.constraintMessagePropertyMarker(psiElement, results);
            }
        }
    }

    private void classNameMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> result, Supplier<ContainerCollectionResolver.ServiceCollector> serviceCollectorSupplier) {
        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass) || ((PhpClass) phpClassContext).isAbstract()) {
            return;
        }

        ContainerCollectionResolver.ServiceCollector serviceCollector = serviceCollectorSupplier.get();
        Set<String> serviceNames = serviceCollector.convertClassNameToServices(((PhpClass) phpClassContext).getFQN());
        if (serviceNames.isEmpty()) {
            return;
        }

        Image serviceLineMarker = PlatformIconGroup.nodesTask(); // TODO consulo-port: ExternalSystemIcons.Task missing, using PlatformIconGroup.nodesTask()
        Collection<ClassServiceDefinitionTargetLazyValue> targets = new ArrayList<>();
        Collection<String> tags = new HashSet<>();
        targets.add(new ClassServiceDefinitionTargetLazyValue(project, ((PhpClass) phpClassContext).getFQN()));

        if (hasResourcePrototypeMetadata(serviceNames, serviceCollector)) {
            // TODO consulo-port: no SourceRootFileLayer icon; badge position (SwingConstants.CENTER) not supported
            Image serviceLineMarkerLayer = ImageEffects.layered(serviceLineMarker, PlatformIconGroup.modulesSourceroot());

            serviceLineMarker = serviceLineMarkerLayer;
        }

        for (String serviceName : serviceNames) {
            tags.addAll(ServiceUtil.getServiceTags(project, serviceName));

            ContainerService containerService = serviceCollector.getServices().get(serviceName);
            if (containerService == null) {
                continue;
            }

            tags.addAll(containerService.getTags());
        }

        if (!tags.isEmpty()) {
            // TODO consulo-port: badge position (SwingConstants.CENTER) not supported
            Image serviceLineMarkerLayer = ImageEffects.layered(serviceLineMarker, PlatformIconGroup.nodesTabpin());

            serviceLineMarker = serviceLineMarkerLayer;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(serviceLineMarker)
            .setTargets(NotNullLazyValue.createValue(new MyCollectionNotNullLazyValue(targets)))
            .setTooltipText("Navigate to definition");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void entityClassMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> result) {
        PsiElement phpClassContext = psiElement.getContext();
        if (!(phpClassContext instanceof PhpClass phpClass)) {
            return;
        }

        Collection<PsiFile> psiFiles = new ArrayList<>();
        for (PsiFile metadataFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, DoctrineMetadataUtil.findMetadataFiles(project, phpClass.getFQN()))) {
            // prevent self navigation for line marker
            if (metadataFile instanceof PhpFile) {
                continue;
            }

            psiFiles.add(metadataFile);
        }

        if (psiFiles.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(psiFiles).
            setTooltipText("Navigate to model");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void repositoryClassMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> result) {

        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        Collection<PsiFile> psiFiles = new ArrayList<>();
        for (VirtualFile virtualFile : DoctrineMetadataUtil.findMetadataForRepositoryClass((PhpClass) phpClassContext)) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(file == null) {
                continue;
            }

            psiFiles.add(file);
        }

        if(psiFiles.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(psiFiles).
            setTooltipText("Navigate to metadata");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void formNameMarker(PsiElement psiElement, Collection<LineMarkerInfo> result) {

        if(!(psiElement instanceof StringLiteralExpression)) {
            return;
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if(method == null) {
            return;
        }

        if(PhpElementsUtil.isMethodInstanceOf(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "getParent")) {
            // get form string; on blank string we dont need any further action

            String contents = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isBlank(contents)) {
                return;
            }

            PsiElement formPsiTarget = FormUtil.getFormTypeToClass(psiElement.getProject(), contents);
            if(formPsiTarget != null) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER).
                    setTargets(formPsiTarget).
                    setTooltipText("Navigate to form type");

                result.add(builder.createLineMarkerInfo(psiElement));
            }

        }

    }

    /**
     * Constraints in same namespace and validateBy service name
     */
    private void validatorClassMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> results) {
        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass) || !PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Validator\\Constraint")) {
            return;
        }

        // class in same namespace
        String className = ((PhpClass) phpClassContext).getFQN() + "Validator";
        Collection<PhpClass> phpClasses = new ArrayList<>(PhpElementsUtil.getClassesInterface(project, className));

        // @TODO: validateBy alias

        if(phpClasses.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER).
            setTargets(phpClasses).
            setTooltipText("Navigate to validator");

        results.add(builder.createLineMarkerInfo(psiElement));
    }

    /**
     * "FooValidator" back to "Foo" constraint
     */
    private void constraintValidatorClassMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> results) {
        PsiElement phpClass = psiElement.getContext();
        if(!(phpClass instanceof PhpClass) || !PhpElementsUtil.isInstanceOf((PhpClass) phpClass, "Symfony\\Component\\Validator\\ConstraintValidatorInterface")) {
            return;
        }

        String fqn = ((PhpClass) phpClass).getFQN();
        if(!fqn.endsWith("Validator")) {
            return;
        }

        Collection<PhpClass> phpClasses = new ArrayList<>(
            PhpElementsUtil.getClassesInterface(project, fqn.substring(0, fqn.length() - "Validator".length()))
        );

        if(phpClasses.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER).
            setTargets(phpClasses).
            setTooltipText("Navigate to constraint");

        results.add(builder.createLineMarkerInfo(psiElement));
    }

    private void autowireConstructorMarker(Project project, PsiElement psiElement, Collection<LineMarkerInfo> results, Supplier<ContainerCollectionResolver.ServiceCollector> serviceCollectorSupplier) {
        PsiElement method = psiElement.getParent();
        if (!(method instanceof Method)) {
            return;
        }

        if (!"__construct".equals(((Method) method).getName()) || !((Method) method).getAccess().isPublic()) {
            return;
        }

        PhpClass phpClass = ((Method) method).getContainingClass();
        if (phpClass == null || phpClass.isAbstract() || phpClass.isInterface()) {
            return;
        }

        boolean isAutowire = false;

        Collection<ClassServiceDefinitionTargetLazyValue> targets = new ArrayList<>();
        ContainerCollectionResolver.ServiceCollector serviceCollector = serviceCollectorSupplier.get();
        for (String convertClassNameToService : serviceCollector.convertClassNameToServices(phpClass.getFQN())) {
            ContainerService containerService = serviceCollector.getServices().get(convertClassNameToService);
            if (containerService == null) {
                continue;
            }

            if (containerService.isAutowireEnabled()) {
                isAutowire = true;
                targets.add(new ClassServiceDefinitionTargetLazyValue(project, phpClass.getFQN()));
                break;
            }
        }

        if (!isAutowire) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PlatformIconGroup.nodesPlugin())
            .setTargets(NotNullLazyValue.createValue(new MyCollectionNotNullLazyValue(targets)))
            .setTooltipText("Symfony: <a href=\"https://symfony.com/doc/current/service_container/autowiring.html\">Autowire available</a>");

        results.add(builder.createLineMarkerInfo(psiElement));
    }

    private static boolean hasResourcePrototypeMetadata(Collection<String> serviceNames, ContainerCollectionResolver.ServiceCollector serviceCollector) {
        for (String serviceName : serviceNames) {
            ContainerService containerService = serviceCollector.getServices().get(serviceName);
            if (containerService != null && containerService.hasResourcePrototypeMetadata()) {
                return true;
            }
        }

        return false;
    }

    private void constraintMessagePropertyMarker(PsiElement psiElement, Collection<LineMarkerInfo> results) {
        PsiElement parent = psiElement.getParent();
        if (parent instanceof StringLiteralExpression && TranslationUtil.isConstraintPropertyField((StringLiteralExpression) parent)) {
            String contents = ((StringLiteralExpression) parent).getContents();

            if (TranslationUtil.hasTranslationKey(psiElement.getProject(), contents, "validators")) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TRANSLATION)
                    .setTargets(NotNullLazyValue.createValue(() -> Arrays.asList(TranslationUtil.getTranslationPsiElements(psiElement.getProject(), contents, "validators"))))
                    .setTooltipText("Navigate to translation");

                results.add(builder.createLineMarkerInfo(psiElement));
            }
        }
    }

    private record MyCollectionNotNullLazyValue(Collection<ClassServiceDefinitionTargetLazyValue> targets) implements Supplier<Collection<? extends PsiElement>> {
        private MyCollectionNotNullLazyValue(Collection<ClassServiceDefinitionTargetLazyValue> targets) {
            this.targets = targets;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            Collection<PsiElement> myTargets = new HashSet<>();
            targets.stream()
                .map((Function<ClassServiceDefinitionTargetLazyValue, Collection<? extends PsiElement>>) ClassServiceDefinitionTargetLazyValue::get)
                .forEach(myTargets::addAll);
            return myTargets;
        }
    }

    private static final class LazyServiceCollector implements Supplier<ContainerCollectionResolver.ServiceCollector> {
        private final Project project;
        private ContainerCollectionResolver.ServiceCollector serviceCollector;

        private LazyServiceCollector(Project project) {
            this.project = project;
        }

        @Override
        public ContainerCollectionResolver.ServiceCollector get() {
            if (serviceCollector == null) {
                serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project);
            }

            return serviceCollector;
        }
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
