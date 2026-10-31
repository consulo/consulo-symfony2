package fr.adrienbrault.idea.symfony2plugin.util.dict;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.codeEditor.Editor;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ResourceFileInsertHandler implements InsertHandler<LookupElement> {

    private static final ResourceFileInsertHandler instance = new ResourceFileInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        // resource: "@AsseticBundle/Resources/config/filters/cssimport.xml"
        // if no @ is before cursor add one
        if(!isStringBeforeCaret(context.getEditor(), context, "@")) {
            context.getDocument().insertString(context.getStartOffset(), "@");
        }

    }

    public static boolean isStringBeforeCaret(Editor editor, InsertionContext context, String string) {

        String fileText = editor.getDocument().getText();
        if (fileText.length() < string.length()) {
            return false;
        }

        return fileText.startsWith(string, context.getStartOffset() - string.length());
    }


    public static ResourceFileInsertHandler getInstance(){
        return instance;
    }

}