package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerTagLookupElement extends LookupElement {

    final private String tag;
    private boolean isWeak = false;

    public ContainerTagLookupElement(String tag) {
        this.tag = tag;
    }

    public ContainerTagLookupElement(String tag, boolean isWeak) {
        this.tag = tag;
        this.isWeak = isWeak;
    }

    @Override
    public String getLookupString() {
        return tag;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);
        presentation.setIcon(isWeak ? Symfony2Icons.SERVICE_TAG_WEAK : Symfony2Icons.SERVICE_TAG);
    }
}
