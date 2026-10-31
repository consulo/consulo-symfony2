package fr.adrienbrault.idea.symfony2plugin.config.doctrine;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineTypeLookup extends LookupElement {

    private final String name;
    private InsertHandler<LookupElement> insertHandler = null;

    public DoctrineTypeLookup(String typeName) {
        this.name = typeName;
    }

    public DoctrineTypeLookup(String typeName, InsertHandler<LookupElement> insertHandler) {
        this.name = typeName;
        this.insertHandler = insertHandler;
    }

    @Override
    public String getLookupString() {
        return name;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(Symfony2Icons.DOCTRINE);
        presentation.setTypeText("Doctrine");
        presentation.setTypeGrayed(true);
    }

}