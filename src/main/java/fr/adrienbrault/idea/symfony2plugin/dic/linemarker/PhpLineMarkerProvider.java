package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.jetbrains.php.lang.PhpLanguage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.application.util.NotNullLazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpArrayServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Definition-side line marker for PHP DIC service definitions.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> result) {
        if (psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.getFirst().getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PsiElement firstElement = psiElements.getFirst();
        PsiFile containingFile = firstElement instanceof PsiFile ? (PsiFile) firstElement : firstElement.getContainingFile();
        if (!(containingFile instanceof PhpFile phpFile)) {
            return;
        }

        VirtualFile virtualFile = phpFile.getVirtualFile();
        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues = null;

        for (PsiElement psiElement : psiElements) {
            PsiElement serviceIdElement = getServiceIdElement(psiElement);
            if (serviceIdElement == null) {
                continue;
            }

            PsiElement leafTarget = getLineMarkerLeafTarget(serviceIdElement);
            if (leafTarget == null) {
                continue;
            }

            String serviceId = getNormalizedServiceId(serviceIdElement);
            if (StringUtils.isBlank(serviceId)) {
                continue;
            }

            if (visitArrayServiceAttributeValue(leafTarget, serviceIdElement, serviceId, result)) {
                continue;
            }

            if (visitFluentDecoratesMethod(leafTarget, serviceIdElement, serviceId, result)) {
                continue;
            }

            if (!isServiceDefinitionId(serviceIdElement)) {
                continue;
            }

            visitServiceKeyForResources(project, leafTarget, serviceIdElement, serviceId, virtualFile, result);

            if (lazyDecoratedParentServiceValues == null) {
                lazyDecoratedParentServiceValues = new LazyDecoratedParentServiceValues(project);
            }

            visitServiceKeyForDecorates(project, leafTarget, serviceId, result, lazyDecoratedParentServiceValues);
        }
    }

    @Nullable
    private static PsiElement getServiceIdElement(PsiElement psiElement) {
        if (psiElement instanceof StringLiteralExpression || psiElement instanceof ClassConstantReference) {
            return psiElement;
        }

        return null;
    }

    @Nullable
    private static String getNormalizedServiceId(PsiElement psiElement) {
        return StringUtils.stripStart(ServiceContainerUtil.normalizePhpStringValue(PhpElementsUtil.getStringValue(psiElement)), "\\");
    }

    @Nullable
    private static PsiElement getLineMarkerLeafTarget(PsiElement psiElement) {
        if (psiElement instanceof StringLiteralExpression stringLiteralExpression) {
            return PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteralExpression);
        }

        if (psiElement instanceof ClassConstantReference classConstantReference) {
            PsiElement classReference = classConstantReference.getClassReference();
            if (classReference != null) {
                return PsiTreeUtil.getDeepestFirst(classReference);
            }
        }

        return null;
    }

    /**
     * Accept only keys that belong to a PHP `services` entry like:
     * 'App\\' => ['resource' => '../src/']
     */
    private static boolean isServiceDefinitionId(PsiElement keyElement) {
        return PhpArrayServiceUtil.isServiceKey(keyElement) || isFluentServiceSetId(keyElement);
    }

    /**
     * Accept service ids from fluent config chains like:
     * $services->set('app.mailer')->decorate(...)
     * $services->set(Mailer::class)->decorate(...)
     */
    private static boolean isFluentServiceSetId(PsiElement keyElement) {
        PsiElement parent = keyElement.getParent();
        if (!(parent instanceof ParameterList) || PsiElementUtils.getParameterIndexValue(keyElement) != 0) {
            return false;
        }

        MethodReference methodReference = PsiTreeUtil.getParentOfType(keyElement, MethodReference.class);
        if (methodReference == null || methodReference.getParameterList() != parent || !"set".equals(methodReference.getName())) {
            return false;
        }

        PsiFile containingFile = methodReference.getContainingFile();
        return containingFile instanceof PhpFile phpFile && isContainerConfiguratorNamespace(phpFile);
    }

    private static void visitServiceKeyForResources(Project project, PsiElement leafTarget, PsiElement keyElement, String serviceId, @Nullable VirtualFile virtualFile, Collection<? super LineMarkerInfo<?>> result) {
        if (virtualFile == null || !serviceId.endsWith("\\")) {
            return;
        }

        ArrayHashElement serviceEntry = PsiTreeUtil.getParentOfType(keyElement, ArrayHashElement.class);
        if (serviceEntry == null || !(serviceEntry.getValue() instanceof ArrayCreationExpression)) {
            return;
        }

        ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, virtualFile, serviceId);
        if (service == null || service.getResource().isEmpty()) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(PlatformIconGroup.modulesSourceroot())
            .setTargets(NotNullLazyValue.createValue(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, virtualFile, service)))
            .setTooltipText("Navigate to class")
            .createLineMarkerInfo(leafTarget));
    }

    /**
     * Add reverse gutters on a PHP service key, eg:
     * Mailer::class => null
     * DecoratingMailer::class => ['decorates' => Mailer::class]
     */
    private static void visitServiceKeyForDecorates(Project project, PsiElement leafTarget, String serviceId, Collection<? super LineMarkerInfo<?>> result, LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues) {
        NavigationGutterIconBuilder<PsiElement> decorateLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            serviceId
        );

        if (decorateLineMarker != null) {
            result.add(decorateLineMarker.createLineMarkerInfo(leafTarget));
        }

        NavigationGutterIconBuilder<PsiElement> parentLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedParentServiceValues.getParentServices(),
            serviceId
        );

        if (parentLineMarker != null) {
            result.add(parentLineMarker.createLineMarkerInfo(leafTarget));
        }
    }

    private static boolean visitArrayServiceAttributeValue(PsiElement leafTarget, PsiElement valueElement, String serviceId, Collection<? super LineMarkerInfo<?>> result) {
        String attributeName = getPhpServiceAttributeName(valueElement);
        if (attributeName == null) {
            return false;
        }

        if ("decorates".equals(attributeName)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.DECORATE, serviceId));
            return true;
        }

        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.PARENT, serviceId));
        return true;
    }

    /**
     * Resolve "decorates" / "parent" for PHP array values like:
     * 'decorates' => Mailer::class
     */
    @Nullable
    private static String getPhpServiceAttributeName(PsiElement valueElement) {
        String key = PhpArrayServiceUtil.getServiceAttributeName(valueElement);
        if (!"decorates".equals(key) && !"parent".equals(key)) {
            return null;
        }

        return key;
    }

    /**
     * Add a forward gutter on fluent config, eg:
     * $services->set(DecoratingMailer::class)->decorate(Mailer::class)
     */
    private static boolean visitFluentDecoratesMethod(PsiElement leafTarget, PsiElement parameterElement, String serviceId, Collection<? super LineMarkerInfo<?>> result) {
        PsiElement parent = parameterElement.getParent();
        if (!(parent instanceof ParameterList) || PsiElementUtils.getParameterIndexValue(parameterElement) != 0) {
            return false;
        }

        MethodReference methodReference = PsiTreeUtil.getParentOfType(parameterElement, MethodReference.class);
        if (methodReference == null || methodReference.getParameterList() != parent) {
            return false;
        }

        ServiceUtil.ServiceLineMarker lineMarker = getFluentServiceLineMarker(methodReference);
        if (lineMarker == null || !isFluentServiceDecorateMethod(methodReference)) {
            return false;
        }

        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, lineMarker, serviceId));
        return true;
    }

    private static ServiceUtil.@Nullable ServiceLineMarker getFluentServiceLineMarker(MethodReference methodReference) {
        String name = methodReference.getName();
        if ("decorate".equals(name)) {
            return ServiceUtil.ServiceLineMarker.DECORATE;
        }

        if ("parent".equals(name)) {
            return ServiceUtil.ServiceLineMarker.PARENT;
        }

        return null;
    }

    /**
     * Restrict "decorate(...)" to service configurator chains that originate from ->set(...).
     */
    private static boolean isFluentServiceDecorateMethod(MethodReference methodReference) {
        PsiFile containingFile = methodReference.getContainingFile();
        if (!(containingFile instanceof PhpFile phpFile) || !isContainerConfiguratorNamespace(phpFile)) {
            return false;
        }

        PsiElement classReference = methodReference.getClassReference();
        while (classReference instanceof MethodReference chainedMethodReference) {
            if ("set".equals(chainedMethodReference.getName())) {
                return true;
            }

            classReference = chainedMethodReference.getClassReference();
        }

        return false;
    }

    private static boolean isContainerConfiguratorNamespace(PhpFile phpFile) {
        for (PhpNamespace phpNamespace : PhpNamespaceBraceConverter.getAllNamespaces(phpFile)) {
            if ("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator".equals(phpNamespace.getFQN())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
