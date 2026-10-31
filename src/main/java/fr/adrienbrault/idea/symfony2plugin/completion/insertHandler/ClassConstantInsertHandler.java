package fr.adrienbrault.idea.symfony2plugin.completion.insertHandler;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import com.jetbrains.php.lang.psi.elements.PhpClass;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassConstantInsertHandler implements InsertHandler<LookupElement> {

    private static final ClassConstantInsertHandler instance = new ClassConstantInsertHandler();

    private ClassConstantInsertHandler() {}

    @Override
    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        if(!(lookupElement instanceof ClassConstantLookupElementInterface) || !(lookupElement.getObject() instanceof PhpClass)) {
            return;
        }

        PhpReferenceInsertHandler.getInstance().handleInsert(context, lookupElement);
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "::class");
    }

    public static ClassConstantInsertHandler getInstance(){
        return instance;
    }

    public interface ClassConstantLookupElementInterface {
        PhpClass getPhpClass();
    }
}
