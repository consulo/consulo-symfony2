package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.intentions.xml.XmlServiceSuggestIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceSuggestIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String expectedClass;

    public XmlServiceSuggestIntentionAction(String expectedClass, PsiElement target) {
        super(target);
        this.expectedClass = expectedClass;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Suggest Service");
    }

    @Override
    public void invoke(Project project, PsiFile psiFile, @Nullable Editor editor, PsiElement psiElement, PsiElement psiElement1) {
        if(editor == null) {
            return;
        }

        PhpClass phpClass = PhpElementsUtil.getClass(project, expectedClass);
        if(phpClass == null) {
            return;
        }

        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(phpClass, ContainerCollectionResolver.getServices(project));
        if(suggestions.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No suggestion found");
            return;
        }

        XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(xmlTag == null) {
            return;
        }

        ServiceSuggestDialog.create(
            editor,
            ContainerUtil.map(suggestions, ContainerService::getName),
            new XmlServiceSuggestIntention.MyInsertCallback(xmlTag)
        );
    }
}