package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class QuotedInsertHandler implements InsertHandler<LookupElement> {

    private static final QuotedInsertHandler instance = new QuotedInsertHandler();

    public static QuotedInsertHandler getInstance(){
        return instance;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement lookupElement) {
        if(ResourceFileInsertHandler.isStringBeforeCaret(context.getEditor(), context, "'") ||
           ResourceFileInsertHandler.isStringBeforeCaret(context.getEditor(), context, "\""))
        {
            return;
        }

        int startOffset = context.getStartOffset();
        context.getDocument().insertString(startOffset, "\"");
        context.getDocument().insertString(startOffset + lookupElement.getLookupString().length() + 1, "\"");

        // move to end
        context.getEditor().getCaretModel().moveCaretRelatively(1, 0, false, false, true);
    }
}
