package fr.adrienbrault.idea.symfony2plugin.tests.templating.inlay;

import consulo.it.AllowWriteLockUnderUIThread;

import org.junit.jupiter.api.Test;

import fr.adrienbrault.idea.symfony2plugin.templating.inlay.TwigVariableInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see TwigVariableInsertHandler
 */
// the document is changed on the UI thread like in the IDE
@AllowWriteLockUnderUIThread
public class TwigVariableInsertHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    // --- insertPrintBlock ---

    @Test

    public void testInsertPrintBlockInsertsPrintExpression() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertPrintBlock("product"));
        assertContent("\n{{ product }}");
    }

    @Test

    public void testInsertPrintBlockWithPropertyPath() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertPrintBlock("user.email"));
        assertContent("\n{{ user.email }}");
    }

    @Test

    public void testInsertPrintBlockAppendsAfterExistingContent() {
        myFixture.configureByText("test.html.twig", "{{ title }}<caret>");
        invoke(h -> h.insertPrintBlock("name"));
        assertContent("{{ title }}\n{{ name }}");
    }

    // --- insertIfStatement ---

    @Test

    public void testInsertIfStatementForBoolVariable() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertIfStatement("isActive"));
        assertContent("\n{% if isActive %}\n    \n{% endif %}");
    }

    // --- insertEmptyForeach ---

    @Test

    public void testInsertEmptyForeachPluralS() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("products"));
        assertContent("\n{% for product in products %}\n    \n{% endfor %}");
    }

    @Test

    public void testInsertEmptyForeachPluralIes() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("categories"));
        assertContent("\n{% for category in categories %}\n    \n{% endfor %}");
    }

    @Test

    public void testInsertEmptyForeachPluralEs() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("churches"));
        assertContent("\n{% for church in churches %}\n    \n{% endfor %}");
    }

    @Test

    public void testInsertEmptyForeachNoPlural() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("data"));
        assertContent("\n{% for dataItem in data %}\n    \n{% endfor %}");
    }

    // --- insertFilledForeach ---

    @Test

    public void testInsertFilledForeachWithArrayProperty() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("user", "tags"));
        assertContent("\n{% for tag in user.tags %}\n    {{ tag }}\n{% endfor %}");
    }

    @Test

    public void testInsertFilledForeachSingularizeEndsWithEs() {
        // "roles" singularizes to "role"
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("user", "roles"));
        assertContent("\n{% for role in user.roles %}\n    {{ role }}\n{% endfor %}");
    }

    @Test

    public void testInsertFilledForeachWithIesProperty() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("shop", "categories"));
        assertContent("\n{% for category in shop.categories %}\n    {{ category }}\n{% endfor %}");
    }

    // --- insertForeachPropertyAccess ---

    @Test

    public void testInsertForeachPropertyAccessUsesParentCollection() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertForeachPropertyAccess("dates", "foo"));
        assertContent("\n{% for date in dates %}\n    {{ date.foo }}\n{% endfor %}");
    }

    @Test

    public void testInsertForeachPropertyAccessWithNonPluralParent() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertForeachPropertyAccess("data", "name"));
        assertContent("\n{% for dataItem in data %}\n    {{ dataItem.name }}\n{% endfor %}");
    }

    // --- helpers ---

    private void invoke(java.util.function.Consumer<TwigVariableInsertHandler> action) {
        TwigVariableInsertHandler handler = new TwigVariableInsertHandler(myFixture.getEditor(), myFixture.getFile());
        withoutReadLock(() -> action.accept(handler));
    }

    private void assertContent(String expected) {
        assertEquals(expected, myFixture.getEditor().getDocument().getText());
    }
}
