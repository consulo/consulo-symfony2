package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationMethodInsertHandler implements InsertHandler<LookupElement> {

    private static final AnnotationMethodInsertHandler instance = new AnnotationMethodInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {
        if(lookupElement.getObject() instanceof AnnotationValue) {
            String addText = "=\"\"";

            if(((AnnotationValue) lookupElement.getObject()).getType() == AnnotationValue.Type.Array) {
                addText = "={}";
            }
            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), addText);

        } else {
            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "=\"\"");
        }

        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
    }

    public static AnnotationMethodInsertHandler getInstance(){
        return instance;
    }

}
