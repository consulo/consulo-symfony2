package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationIndex;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationTagInsertHandler implements InsertHandler<LookupElement> {

    private static final AnnotationTagInsertHandler instance = new AnnotationTagInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "()");
        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

        String lookupString = lookupElement.getLookupString();
        if(AnnotationIndex.getControllerAnnotations().containsKey(lookupString)) {
            String useStatement = AnnotationIndex.getControllerAnnotations().get(lookupString).getUse();
            if(null != useStatement) {
                AnnotationUseImporter.insertUse(context, useStatement);
            }
        }

    }

    public static AnnotationTagInsertHandler getInstance(){
        return instance;
    }

}
