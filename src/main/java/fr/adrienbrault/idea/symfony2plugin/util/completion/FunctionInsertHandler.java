package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FunctionInsertHandler implements InsertHandler<LookupElement> {

    private static final FunctionInsertHandler instance = new FunctionInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        // check this:
        // {{ form_javasc|() }}
        // {{ form_javasc| }}
        if(PhpInsertHandlerUtil.isStringAtCaret(context.getEditor(), "(")) {
            return;
        }

        String addText = "()";
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), addText);

        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

    }

    public static FunctionInsertHandler getInstance(){
        return instance;
    }

}
