package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

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
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlLineMarkerProvider implements LineMarkerProvider {
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

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedServices = null;
        var serviceIdKeyValuePattern = YamlElementPatternHelper.getServiceIdKeyValuePattern();

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
                continue;
            }

            PsiElement yamlKeyValue = psiElement.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue) || !serviceIdKeyValuePattern.accepts(yamlKeyValue)) {
                continue;
            }

            if(lazyDecoratedServices == null) {
                lazyDecoratedServices = new LazyDecoratedParentServiceValues(project);
            }

            // services -> service_name
            visitServiceId(project, psiElement, (YAMLKeyValue) yamlKeyValue, result, lazyDecoratedServices);

            // services:
            //    App\:
            //        resource: '../src/*'
            visitServiceIdForResources(project, psiElement, (YAMLKeyValue) yamlKeyValue, result);
        }
    }

    private void visitServiceIdForResources(Project project, PsiElement leafTarget, YAMLKeyValue yamlKeyValue, Collection<? super LineMarkerInfo<?>> result) {
        String id = yamlKeyValue.getKeyText();
        if (StringUtils.isBlank(id) || !id.endsWith("\\")) {
            return;
        }

        PsiFile containingFile = yamlKeyValue.getContainingFile();
        if (containingFile == null) {
            return;
        }

        VirtualFile containerFile = containingFile.getVirtualFile();
        if (containerFile == null) {
            return;
        }

        ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, containerFile, id);
        if (service == null || service.getResource().isEmpty()) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(PlatformIconGroup.modulesSourceroot())
            .setTargets(NotNullLazyValue.createValue(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, containerFile, service)))
            .setTooltipText("Navigate to class")
            .createLineMarkerInfo(leafTarget));
    }

    private void visitServiceId(Project project, PsiElement leafTarget, YAMLKeyValue yamlKeyValue, Collection<? super LineMarkerInfo<?>> result, LazyDecoratedParentServiceValues lazyDecoratedServices) {
        String id = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(id)) {
            return;
        }

        ServiceLineMarkerUtil.collectDecoratesAndParentTargets(
            leafTarget,
            result,
            YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "decorates"),
            YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "parent")
        );

        ServiceLineMarkerUtil.collectDecoratedAndParentTargets(
            project,
            leafTarget,
            result,
            lazyDecoratedServices,
            id
        );
    }

    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
