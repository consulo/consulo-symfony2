package fr.adrienbrault.idea.symfony2plugin.intentions.xml;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.XmlServiceTagIntention", fileExtensions = "xml", categories = "Symfony")
public class XmlServiceTagIntention extends PsiElementBaseIntentionAction {
    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>XML</language>
        if (!psiElement.getLanguage().isKindOf(XMLLanguage.INSTANCE)) {
            return false;
        }

        if (psiElement.getContainingFile().getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        return ServiceActionUtil.getServiceTagValid(psiElement) != null;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        XmlTag xmlTag = ServiceActionUtil.getServiceTagValid(psiElement);
        if (xmlTag == null) {
            return;
        }

        PhpClass phpClassFromXmlTag = ServiceActionUtil.getPhpClassFromXmlTag(xmlTag, new ContainerCollectionResolver.LazyServiceCollector(project));
        if (phpClassFromXmlTag == null) {
            return;
        }

        Set<String> phpServiceTags = ServiceUtil.getPhpClassServiceTags(phpClassFromXmlTag);
        if (phpServiceTags.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "Ops, no possible Tag found");
            return;
        }

        for (XmlTag tag : xmlTag.getSubTags()) {
            if (!"tag".equals(tag.getName())) {
                continue;
            }

            XmlAttribute name = tag.getAttribute("name");
            if (name == null) {
                continue;
            }

            String value = name.getValue();
            phpServiceTags.remove(value);
        }

        ServiceUtil.insertTagWithPopupDecision(editor, phpServiceTags, tag -> {
            ServiceTag serviceTag = new ServiceTag(phpClassFromXmlTag, tag);
            ServiceUtil.decorateServiceTag(serviceTag);
            xmlTag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText(serviceTag.toXmlString()), false);
        });
    }


    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add Tags");
    }
}
