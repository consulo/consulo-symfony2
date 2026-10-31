package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementFactory;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.IntentionAndQuickFixAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractGuessTypoQuickFix extends IntentionAndQuickFixAction {

    /**
     * Short label for this suggestion type, e.g. "Route", "Template", "Translation Key".
     * Used as the WriteCommandAction name ("<label> Suggestion") and popup title ("Symfony: <label> Suggestions").
     */
    protected abstract String getSuggestionLabel();

    /**
     * Returns the ranked list of candidate replacements for the missing value.
     */
    protected abstract List<String> getSimilarItems(Project project);

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony: Apply Similar Suggestion");
    }

    @Override
    public void applyFix(Project project, PsiFile psiFile, @Nullable Editor editor) {
        if (editor == null) {
            return;
        }

        PsiElement elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        List<String> similarItems = getSimilarItems(project);
        if (similarItems.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "No similar item found");
            return;
        }

        String actionName = getSuggestionLabel() + " Suggestion";
        Consumer<String> suggestionSelected = null;

        PsiElement parent = elementAt.getParent();
        if (elementAt.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
            // TWIG
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, actionName, null, () -> {
                PsiElement firstFromText = TwigElementFactory.createPsiElement(project, "{% foo '" + selectedValue + "' }%", TwigTokenTypes.STRING_TEXT);
                elementAt.replace(firstFromText);
            });
        } else if (parent instanceof StringLiteralExpression) {
            // PHP + DocTag
            suggestionSelected = selectedValue -> WriteCommandAction.runWriteCommandAction(project, actionName, null, () -> {
                String contents = parent.getText();
                String wrap = "'";
                if (!contents.isEmpty()) {
                    String wrap2 = contents.substring(0, 1);
                    if (wrap2.equals("\"") || wrap2.equals("'")) {
                        wrap = wrap2;
                    }
                }

                StringLiteralExpression firstFromText = PhpPsiElementFactory.createFirstFromText(project, StringLiteralExpression.class, wrap + selectedValue + wrap);
                parent.replace(firstFromText);
            });
        }

        if (suggestionSelected == null) {
            IdeHelper.showErrorHintIfAvailable(editor, "No replacement provider found");
            return;
        }

        if (similarItems.size() == 1 || ApplicationManager.getApplication().isHeadlessEnvironment()) {
            suggestionSelected.accept(similarItems.get(0));
            return;
        }

        editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(similarItems)
            .setTitle("Symfony: " + getSuggestionLabel() + " Suggestions")
            .setItemChosenCallback(suggestionSelected)
            .createPopup());
    }
}
