package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import org.junit.jupiter.api.Test;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.FilterGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see FilterGotoCompletionRegistrar
 */
public class FilterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("FilterGotoCompletionRegistrarTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    @Test

    public void testCompletionForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter <caret> %}", "foobar");
    }

    @Test

    public void testNavigationForTwigFilterTagIdentifier() {
        assertCompletionContains(TwigFileType.INSTANCE, "{% filter foo<caret>bar %}", "foobar");
    }
}
