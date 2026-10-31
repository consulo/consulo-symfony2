package fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention;

import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractServiceSuggestIntentionAction extends LocalQuickFixAndIntentionActionOnPsiElement {
    protected final String expectedClass;

    protected AbstractServiceSuggestIntentionAction(String expectedClass, PsiElement psiElement) {
        super(psiElement);
        this.expectedClass = expectedClass;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Suggest Service");
    }

    protected abstract ServiceSuggestDialog.Callback createInsertCallback(Editor editor, PsiElement psiElement);

    @Override
    public void invoke(Project project, PsiFile psiFile, @Nullable Editor editor, PsiElement psiElement, PsiElement psiElement1) {
        if (editor == null) {
            return;
        }

        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(this.expectedClass);
        if (anyByFQN.isEmpty()) {
            return;
        }

        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(anyByFQN.iterator().next(), ContainerCollectionResolver.getServices(project));
        if (suggestions.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No suggestion found");
            return;
        }

        ServiceSuggestDialog.create(
            editor,
            ContainerUtil.map(suggestions, ContainerService::getName),
            createInsertCallback(editor, psiElement)
        );
    }
}
