package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QuotedInsertionLookupElement extends LookupElement {

    private final LookupElement lookupElement;

    public QuotedInsertionLookupElement(LookupElement lookupElement) {
        this.lookupElement = lookupElement;
    }

    @Override
    public String getLookupString() {
        return this.lookupElement.getLookupString();
    }

    @Nullable
    @Override
    public PsiElement getPsiElement() {
        return lookupElement.getPsiElement();
    }

    @Override
    public boolean isValid() {
        return lookupElement.isValid();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        QuotedInsertHandler.getInstance().handleInsert(context, this);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        lookupElement.renderElement(presentation);
    }
}
