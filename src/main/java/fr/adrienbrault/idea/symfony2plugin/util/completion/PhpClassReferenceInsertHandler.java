package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;

/**
* simplified PhpReferenceInsertHandler which not use trailing quote
*/
public class PhpClassReferenceInsertHandler implements InsertHandler<LookupElement> {

    private static final PhpClassReferenceInsertHandler instance = new PhpClassReferenceInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {
        Object object = lookupElement.getObject();

        if (!(object instanceof PhpClass aClass)) {
            return;
        }

        StringBuilder textToInsertBuilder = new StringBuilder();
        String fqn = aClass.getNamespaceName();

        if(fqn.startsWith("\\")) {
            fqn = fqn.substring(1);
        }

        textToInsertBuilder.append(fqn);
        context.getDocument().insertString(context.getStartOffset(), textToInsertBuilder);

    }

    public static PhpClassReferenceInsertHandler getInstance(){
        return instance;
    }

}
