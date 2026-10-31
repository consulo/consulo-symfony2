package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DoctrineMetadataLineMarkerProvider implements LineMarkerProvider {

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

        Project project = psiElements.get(0).getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PsiFile containingFile = psiElements.get(0).getContainingFile();
        if (containingFile == null || !XmlHelper.isXmlFileExtension(containingFile)) {
            return;
        }

        var xmlTargetDocumentPattern = DoctrineMetadataPattern.getXmlTargetDocumentClass();
        var xmlTargetEntityPattern = DoctrineMetadataPattern.getXmlTargetEntityClass();
        var embeddableNamePattern = DoctrineMetadataPattern.getEmbeddableNameClassPattern();

        for(PsiElement psiElement: psiElements) {
            if(psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
                continue;
            }

            PsiElement xmlAttributeValue = psiElement.getParent();
            if(xmlAttributeValue instanceof XmlAttributeValue && (xmlTargetDocumentPattern.accepts(xmlAttributeValue) || xmlTargetEntityPattern.accepts(xmlAttributeValue) || embeddableNamePattern.accepts(xmlAttributeValue))) {
                attachXmlRelationMarker(psiElement, (XmlAttributeValue) xmlAttributeValue, results);
            }
        }
    }

    private void attachXmlRelationMarker(PsiElement target, XmlAttributeValue psiElement, Collection<LineMarkerInfo> results) {
        String value = psiElement.getValue();
        if(StringUtils.isBlank(value)) {
            return;
        }

        Collection<PhpClass> classesInterface = DoctrineMetadataUtil.getClassInsideScope(psiElement, value);
        if(classesInterface.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(classesInterface).
            setTooltipText("Navigate to class");

        results.add(builder.createLineMarkerInfo(target));
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
