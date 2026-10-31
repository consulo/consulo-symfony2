package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import org.junit.jupiter.api.Test;

import fr.adrienbrault.idea.symfony2plugin.tests.JUnit3Assert;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;

import java.util.Map;

/**
 * @see TwigTemplateCompletionContributor#decorateFormFieldLookupElement
 */
public class TwigFormFieldLookupElementTest extends JUnit3Assert {
    @Test
    public void testFormFieldCompletionUsesPrimitiveFormDataHolderPresentation() {
        LookupElement lookupElement = TwigTemplateCompletionContributor.decorateFormFieldLookupElement(
            LookupElementBuilder.create("title"),
            new FormFieldDataHolder("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", "\\App\\Form\\ProductType", null, Map.of())
        );

        LookupElementPresentation presentation = new LookupElementPresentation();
        lookupElement.renderElement(presentation);

        assertEquals("title", lookupElement.getLookupString());
        assertEquals("TextType", presentation.getTypeText());
        assertEquals("(ProductType)", presentation.getTailText());
    }
}
