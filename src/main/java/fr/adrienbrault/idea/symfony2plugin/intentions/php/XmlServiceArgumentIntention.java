package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.XmlServiceArgumentIntention", fileExtensions = "xml", categories = "Symfony")
public class XmlServiceArgumentIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        XmlTag xmlTag = ServiceActionUtil.getServiceTagValid(psiElement);
        if (xmlTag == null) {
            return;
        }

        List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, true, new ContainerCollectionResolver.LazyServiceCollector(project));
        if (args.isEmpty()) {
            return;
        }

        ServiceActionUtil.fixServiceArgument(args, xmlTag);
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>XML</language>
        if (!psiElement.getLanguage().isKindOf(XMLLanguage.INSTANCE)) {
            return false;
        }

        if (psiElement.getContainingFile().getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        XmlTag serviceTagValid = ServiceActionUtil.getServiceTagValid(psiElement);
        if (serviceTagValid == null) {
            return false;
        }

        if (!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTagValid)) {
            return false;
        }

        return !ServiceActionUtil.getXmlMissingArgumentTypes(serviceTagValid, true, new ContainerCollectionResolver.LazyServiceCollector(project)).isEmpty();
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add Arguments");
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
