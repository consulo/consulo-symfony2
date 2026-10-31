package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorLookupElement extends LookupElement {
    private final String translationString;

    private final String domain;

    @Nullable
    private InsertHandler<TranslatorLookupElement> insertHandler;

    private boolean isWeak;

    public TranslatorLookupElement(String translationString, String domain) {
        this.translationString = translationString;
        this.domain = domain;
    }

    public TranslatorLookupElement(String translationString, String domain, InsertHandler<TranslatorLookupElement> insertHandler) {
        this.translationString = translationString;
        this.domain = domain;
        this.insertHandler = insertHandler;
    }

    public TranslatorLookupElement(String translationString, String domain, boolean isWeak) {
        this(translationString, domain);
        this.isWeak = isWeak;
    }

    @Override
    public String getLookupString() {
        return translationString;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(domain);
        presentation.setTypeGrayed(true);

        if(this.isWeak) {
            presentation.setIcon(Symfony2Icons.TRANSLATION_WEAK);
        } else {
            presentation.setIcon(Symfony2Icons.TRANSLATION);
        }
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if(insertHandler != null) {
            insertHandler.handleInsert(context, this);
        }
    }

    public String getDomain() {
        return domain;
    }
}
