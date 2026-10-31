package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationFilterInsertHandler implements InsertHandler<TranslatorLookupElement> {
    private static final TwigTranslationFilterInsertHandler instance = new TwigTranslationFilterInsertHandler();

    public void handleInsert(InsertionContext context, TranslatorLookupElement lookupElement) {
        PsiElement elementAt = context.getFile().findElementAt(context.getStartOffset());

        String domain = null;

        if(elementAt != null) {
            PsiElement element = TwigUtil.getElementOnTwigViewProvider(elementAt);
            TwigUtil.DomainScope domainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : elementAt);
            String transDefaultDomain = domainScope.getDefaultDomain();

            String myDomain = lookupElement.getDomain();
            if(!transDefaultDomain.equalsIgnoreCase(myDomain)) {
                domain = myDomain;
            }
        }

        // preappend
        context.getDocument().insertString(context.getStartOffset(), "{{ '");

        StringBuilder stringBuilder = new StringBuilder();

        // 'foo.bar'
        stringBuilder.append("'|trans");

        // domain
        if(domain != null) {
            stringBuilder.append(String.format("({}, '%s')", domain));
        }

        stringBuilder.append(" }}");

        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), stringBuilder.toString());
    }

    public static TwigTranslationFilterInsertHandler getInstance(){
        return instance;
    }
}