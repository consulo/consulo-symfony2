package fr.adrienbrault.idea.symfony2plugin.routing;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.pattern.XmlPatterns;
import consulo.xml.language.psi.pattern.XmlTagPattern;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> lineMarkerInfos) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        var xmlTagNameLeafPattern = XmlHelper.getXmlTagNameLeafStartPattern();
        var routeTagPattern = Pattern.getRouteTag();
        var routeImportPattern = Pattern.getRouteImport();

        for(PsiElement psiElement: psiElements) {
            if(xmlTagNameLeafPattern.accepts(psiElement)) {
                attachRouteActions(psiElement, lineMarkerInfos, routeTagPattern);
                attachRouteImport(psiElement, lineMarkerInfos, routeImportPattern);



            } else if(psiElement instanceof XmlFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }

    }

    private void attachRouteActions(PsiElement psiElement, Collection<LineMarkerInfo> lineMarkerInfos, XmlTagPattern.Capture routeTagPattern) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !routeTagPattern.accepts(xmlTag)) {
            return;
        }

        String controller = RouteHelper.getXmlController((XmlTag) xmlTag);
        if (controller != null) {
            PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(xmlTag.getProject(), controller);
            if (methods.length > 0) {
                RouteHelper.addControllerLineMarker(lineMarkerInfos, methods, psiElement);
            }
        }
    }

    private void attachRouteImport(PsiElement psiElement, Collection<LineMarkerInfo> lineMarkerInfos, XmlTagPattern.Capture routeImportPattern) {
        PsiElement xmlTag = psiElement.getParent();
        if(!(xmlTag instanceof XmlTag) || !routeImportPattern.accepts(xmlTag)) {
            return;
        }

        String type = ((XmlTag) xmlTag).getAttributeValue("type");
        if (!"annotation".equals(type) && !"attribute".equals(type)) {
            return;
        }

        String resource = ((XmlTag) xmlTag).getAttributeValue("resource");
        if (StringUtils.isBlank(resource)) {
            return;
        }

        lineMarkerInfos.add(
            FileResourceUtil.getNavigationGutterForRouteAnnotationResources(
                psiElement.getProject(),
                psiElement.getContainingFile().getVirtualFile(),
                resource
            ).createLineMarkerInfo(psiElement)
        );
    }

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    private static class Pattern {
        // Matches <route> elements directly inside a <routes> container in XML routing files
        static XmlTagPattern.Capture getRouteTag() {
            return XmlPatterns.xmlTag().withName("route").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }

        // Matches <import> elements directly inside a <routes> container in XML routing files
        static XmlTagPattern.Capture getRouteImport() {
            return XmlPatterns.xmlTag().withName("import").withParent(
                XmlPatterns.xmlTag().withName("routes")
            ).inFile(XmlHelper.getXmlFilePattern());
        }
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
