package fr.adrienbrault.idea.symfony2plugin.intentions.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.XmlServiceSuggestIntention", fileExtensions = "xml", categories = "Symfony")
public class XmlServiceSuggestIntention extends PsiElementBaseIntentionAction {
    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>XML</language>
        if (!psiElement.getLanguage().isKindOf(XMLLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if (argumentTag == null || !"argument".equals(argumentTag.getName())) {
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

        String aClass = XmlHelper.getClassFromServiceDefinition(xmlTag);
        if (aClass == null || StringUtils.isBlank(aClass)) {
            return;
        }

        XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if (argumentTag == null) {
            return;
        }

        int argumentIndex = XmlHelper.getArgumentIndex(argumentTag);

        Set<String> suggestions = ServiceUtil.getServiceSuggestionsForServiceConstructorIndex(project, aClass, argumentIndex);
        if (suggestions.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No suggestion found");
            return;
        }

        ServiceSuggestDialog.create(editor, suggestions, new MyInsertCallback(argumentTag));
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Suggest Service");
    }

    public static class MyInsertCallback implements ServiceSuggestDialog.Callback {
        private final XmlTag myArgumentTag;

        public MyInsertCallback(XmlTag argumentTag) {
            myArgumentTag = argumentTag;
        }

        @Override
        public void insert(String selected) {
            // set type="service" for lazy devs
            if (ContainerUtil.find(myArgumentTag.getAttributes(), xmlAttribute -> "type".equals(xmlAttribute.getName())) == null) {
                myArgumentTag.setAttribute("type", "service");
            }

            // append type="SERVICE"
            myArgumentTag.setAttribute("id", selected);
        }
    }
}
