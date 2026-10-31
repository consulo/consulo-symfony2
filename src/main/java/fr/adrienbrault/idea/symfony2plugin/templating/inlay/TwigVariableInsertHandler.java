package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiFile;
import consulo.util.lang.StringUtil;

/**
 * Inserts Twig code snippets at the current caret position.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableInsertHandler {

    private final Editor editor;
    private final PsiFile file;

    public TwigVariableInsertHandler(Editor editor, PsiFile file) {
        this.editor = editor;
        this.file = file;
    }

    /** Insert {{ variable }} */
    public void insertPrintBlock(String expression) {
        insertAtCaret("{{ " + expression + " }}");
    }

    /** Insert {% if variable %}...{% endif %} */
    public void insertIfStatement(String variable) {
        insertAtCaret("{% if " + variable + " %}\n    \n{% endif %}");
    }

    /** Insert {% for item in variable %}...{% endfor %} */
    public void insertEmptyForeach(String variable) {
        String item = singularize(variable);
        insertAtCaret("{% for " + item + " in " + variable + " %}\n    \n{% endfor %}");
    }

    /** Insert {% for item in variable.property %}{{ item }}{% endfor %} */
    public void insertFilledForeach(String variable, String property) {
        String item = singularize(property);
        insertAtCaret("{% for " + item + " in " + variable + "." + property + " %}\n    {{ " + item + " }}\n{% endfor %}");
    }

    /** Insert {% for item in variable %}{{ item.property }}{% endfor %} */
    public void insertForeachPropertyAccess(String variable, String property) {
        String item = singularize(variable);
        insertAtCaret("{% for " + item + " in " + variable + " %}\n    {{ " + item + "." + property + " }}\n{% endfor %}");
    }

    private void insertAtCaret(String text) {
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            int lineNumber = document.getLineNumber(caretOffset);
            int lineEnd = document.getLineEndOffset(lineNumber);

            document.insertString(lineEnd, "\n" + text);

            int newOffset = lineEnd + 1 + findInnerOffset(text);
            editor.getCaretModel().moveToOffset(newOffset);
        });
    }

    private int findInnerOffset(String text) {
        int innerLine = text.indexOf("\n    \n");
        if (innerLine >= 0) return innerLine + 5;
        return text.length();
    }

    private static String singularize(String word) {
        if (word.endsWith("s")) {
            String unpluralized = StringUtil.unpluralize(word);
            if (unpluralized != null && !unpluralized.isBlank() && !unpluralized.equals(word)) {
                return unpluralized;
            }
        }

        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";
        if (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word + "Item";
    }
}
