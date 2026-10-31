package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeInsertHandler implements InsertHandler<LookupElement> {

    private static final TwigTypeInsertHandler instance = new TwigTypeInsertHandler();

    public void handleInsert(InsertionContext context, LookupElement lookupElement) {

        PsiElement psiElement = lookupElement.getPsiElement();
        if(psiElement instanceof Method && needParameter((Method) psiElement)) {

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


    }

    private boolean needParameter(Method method) {

        for(Parameter parameter: method.getParameters()) {
            if(!parameter.isOptional()) {
                return true;
            }
        }

        return false;
    }

    public static TwigTypeInsertHandler getInstance(){
        return instance;
    }

}
