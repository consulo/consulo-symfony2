package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpReferenceTrimBackslashInsertHandler implements InsertHandler<LookupElement> {

    private static final PhpReferenceTrimBackslashInsertHandler instance = new PhpReferenceTrimBackslashInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        // reuse core class + namespace insertHandler
        PhpReferenceInsertHandler.getInstance().handleInsert(context, lookupElement);

        // phpstorm8: remove leading backslash on PhpReferenceInsertHandler
        String backslash = context.getDocument().getText(new TextRange(context.getStartOffset(), context.getStartOffset() + 1));
        if("\\".equals(backslash)) {
            context.getDocument().deleteString(context.getStartOffset(), context.getStartOffset() + 1);
        }

    }

    public static PhpReferenceTrimBackslashInsertHandler getInstance(){
        return instance;
    }

}

