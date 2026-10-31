package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Shared helpers for service line markers across XML and YAML providers.
 */
final class ServiceLineMarkerUtil {
    private ServiceLineMarkerUtil() {
    }

    /**
     * Collect forward line markers for direct service references:
     * <service decorates="foobar" />, <service parent="foobar" />,
     * decorates: foobar, parent: foobar
     */
    public static void collectDecoratesAndParentTargets(
        PsiElement leafTarget,
        Collection<? super LineMarkerInfo<?>> result,
        @Nullable String decorates,
        @Nullable String parent
    ) {
        // <service decorates="foobar" /> / decorates: foobar
        if (StringUtils.isNotBlank(decorates)) {
            addDirectLineMarker(leafTarget, result, ServiceUtil.ServiceLineMarker.DECORATE, decorates);
        }

        // <service parent="foobar" /> / parent: foobar
        if (StringUtils.isNotBlank(parent)) {
            addDirectLineMarker(leafTarget, result, ServiceUtil.ServiceLineMarker.PARENT, parent);
        }
    }

    /**
     * Collect reverse line markers for services that reference the current service
     * via "decorates" or "parent".
     */
    public static void collectDecoratedAndParentTargets(
        Project project,
        PsiElement leafTarget,
        Collection<? super LineMarkerInfo<?>> result,
        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues,
        String serviceId
    ) {
        // foreign "decorates" linemarker
        addRelatedLineMarker(
            project,
            leafTarget,
            result,
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            serviceId
        );

        // foreign "parent" linemarker
        addRelatedLineMarker(
            project,
            leafTarget,
            result,
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedParentServiceValues.getParentServices(),
            serviceId
        );
    }

    private static void addDirectLineMarker(
        PsiElement leafTarget,
        Collection<? super LineMarkerInfo<?>> result,
        ServiceUtil.ServiceLineMarker lineMarker,
        String serviceId
    ) {
        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, lineMarker, serviceId));
    }

    private static void addRelatedLineMarker(
        Project project,
        PsiElement leafTarget,
        Collection<? super LineMarkerInfo<?>> result,
        ServiceUtil.ServiceLineMarker lineMarker,
        Map<String, Collection<ContainerService>> serviceIds,
        String serviceId
    ) {
        NavigationGutterIconBuilder<PsiElement> lineMarkerBuilder = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            lineMarker,
            serviceIds,
            serviceId
        );

        if (lineMarkerBuilder != null) {
            result.add(lineMarkerBuilder.createLineMarkerInfo(leafTarget));
        }
    }
}
