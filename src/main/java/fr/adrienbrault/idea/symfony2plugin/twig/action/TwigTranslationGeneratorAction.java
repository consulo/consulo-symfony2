package fr.adrienbrault.idea.symfony2plugin.twig.action;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony.TwigTranslationGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class TwigTranslationGeneratorAction extends CodeInsightAction {
    public TwigTranslationGeneratorAction() {
        super(LocalizeValue.localizeTODO("Translation"), LocalizeValue.empty(), Symfony2Icons.TRANSLATION);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return Symfony2ProjectComponent.isEnabled(project) &&
            (file instanceof TwigFile ||
                file instanceof HtmlFileImpl && StringUtil.endsWithIgnoreCase(file.getName(), ".twig") ||
                TwigUtil.getInjectedTwigElement(file, editor) != null);
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor);
            if (psiElement == null) {
                return;
            }

            PsiFile containingFile = psiElement.getContainingFile();
            if (!(containingFile instanceof TwigFile)) {
                return;
            }

            PsiElement element = TwigUtil.getElementOnTwigViewProvider(psiElement);
            TwigUtil.DomainScope twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : psiElement);

            String defaultDomain = twigFileDomainScope.getDefaultDomain();
            String domain = twigFileDomainScope.getDomain();

            List<String> list = TranslationUtil.getTranslationLookupElementsOnDomain(project, domain)
                .stream()
                .map(LookupElement::getLookupString)
                .sorted()
                .collect(Collectors.toList());

            JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(list)
                .setTitle("Symfony: Translations \"" + StringUtils.abbreviate(domain, 20) + "\"")
                .setItemChosenCallback(selectedValue ->
                    WriteCommandAction.runWriteCommandAction(
                        editor.getProject(),
                        "Symfony: Add Translation \"" + StringUtils.abbreviate(selectedValue, 20) + "\"",
                        null,
                        () -> PhpInsertHandlerUtil.insertStringAtCaret(
                            editor,
                            TwigTranslationGeneratorActionKt.createTranslationSnippet(selectedValue, defaultDomain, domain)
                        )
                    )
                )
                .createPopup();

            editor.showPopupInBestPositionFor(popup);
        }
    }
}
