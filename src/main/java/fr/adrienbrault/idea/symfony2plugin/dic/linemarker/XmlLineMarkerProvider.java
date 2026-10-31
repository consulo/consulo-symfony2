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
import consulo.xml.language.psi.pattern.XmlPatterns;
import consulo.xml.language.psi.pattern.XmlTagPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlLineMarkerProvider implements LineMarkerProvider {
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

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues = null;
        var xmlTagNameLeafPattern = XmlHelper.getXmlTagNameLeafStartPattern();
        var serviceIdPattern = getServiceIdPattern();
        var prototypeNamespacePattern = getPrototypeNamespacePattern();

        for (PsiElement psiElement : psiElements) {
            if(!xmlTagNameLeafPattern.accepts(psiElement)) {
                continue;
            }

            PsiElement xmlTag = psiElement.getParent();
            if(!(xmlTag instanceof XmlTag)) {
                continue;
            }

            if (serviceIdPattern.accepts(xmlTag)) {
                if(lazyDecoratedParentServiceValues == null) {
                    lazyDecoratedParentServiceValues = new LazyDecoratedParentServiceValues(project);
                }

                // <services><service id="foo"/></services>
                visitServiceId(project, psiElement, (XmlTag) xmlTag, result, lazyDecoratedParentServiceValues);

                continue;
            }

            if (prototypeNamespacePattern.accepts(xmlTag)) {
                String namespace = ((XmlTag) xmlTag).getAttributeValue("namespace");
                if (StringUtils.isBlank(namespace)) {
                    continue;
                }

                VirtualFile containerFile = containingFile.getVirtualFile();
                if (containerFile == null) {
                    continue;
                }

                ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, containerFile, namespace);
                if (service == null || service.getResource().isEmpty()) {
                    continue;
                }

                result.add(NavigationGutterIconBuilder.create(PlatformIconGroup.modulesSourceroot())
                    .setTargets(NotNullLazyValue.createValue(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, containerFile, service)))
                    .setTooltipText("Navigate to class")
                    .createLineMarkerInfo(psiElement));
            }
        }
    }

    /**
     * <service id="foo"/>
     */
    private void visitServiceId(Project project, PsiElement leafTarget, XmlTag xmlTag, Collection<? super LineMarkerInfo<?>> result, LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues) {
        String id = xmlTag.getAttributeValue("id");
        if(StringUtils.isBlank(id)) {
            return;
        }

        ServiceLineMarkerUtil.collectDecoratesAndParentTargets(
            leafTarget,
            result,
            xmlTag.getAttributeValue("decorates"),
            xmlTag.getAttributeValue("parent")
        );

        ServiceLineMarkerUtil.collectDecoratedAndParentTargets(
            project,
            leafTarget,
            result,
            lazyDecoratedParentServiceValues,
            id
        );
    }

    /**
     * <service id="%foo.class%"/>
     */
    private static XmlTagPattern.Capture getServiceIdPattern() {
        return XmlPatterns.xmlTag().withName("service")
            .withChild(XmlPatterns.xmlAttribute().withName("id")).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <prototype namespace="App\" resource="../src/*" exclude="../src/{DependencyInjection,Entity,Tests,Kernel.php}"/>
     */
    private static XmlTagPattern.Capture getPrototypeNamespacePattern() {
        return XmlPatterns.xmlTag().withName("prototype")
            .withChild(XmlPatterns.xmlAttribute().withName("namespace")).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
