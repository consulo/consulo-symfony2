package fr.adrienbrault.idea.symfony2plugin.completion.insertHandler;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpClassMember;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * "Foo\Foo" => "Foo\\Foo"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigEscapedSlashInsertHandler implements InsertHandler<LookupElement> {
    private static final TwigEscapedSlashInsertHandler instance = new TwigEscapedSlashInsertHandler();

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        if (!(item.getObject() instanceof SmartPsiElementPointer<?> smartPsiElementPointer)) {
            return;
        }

        String insertString = "";
        PsiElement element = smartPsiElementPointer.getElement();
        if (element instanceof PhpClassMember phpClassMember) {
            String classFqn = Objects.requireNonNull(phpClassMember.getContainingClass()).getFQN();
            String fieldName = phpClassMember.getName();

            insertString = StringUtils.stripStart(classFqn, "\\").replace("\\", "\\\\") + "::" + fieldName;
        } else if (element instanceof PhpClass phpClass) {
            insertString = StringUtils.stripStart(phpClass.getFQN(), "\\").replace("\\", "\\\\");
        }

        Document document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());

        document.insertString(context.getStartOffset(), insertString);
        context.commitDocument();

        PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        context.getEditor().getCaretModel().moveCaretRelatively(insertString.length(), 0, false, false, true);
    }

    public static TwigEscapedSlashInsertHandler getInstance(){
        return instance;
    }
}
