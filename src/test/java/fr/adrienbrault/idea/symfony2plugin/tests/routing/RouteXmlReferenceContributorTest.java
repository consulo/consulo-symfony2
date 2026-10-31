package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import org.junit.jupiter.api.Test;

import consulo.xml.language.XmlFileType;
import consulo.language.pattern.PlatformPatterns;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.RouteXmlReferenceContributor
 */
public class RouteXmlReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("RouteXmlReferenceContributor.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    @Test

    public void testThatRouteLineMarkerForControllerIsGiven() {
        assertReferenceMatchOnParent(
            "foo.xml",
            "<routes><route controller=\"Fo<caret>o\\Bar\"/></routes>",
            PlatformPatterns.psiElement().withName("Bar")
        );

        assertReferenceMatch(
            XmlFileType.INSTANCE,
            "<routes><route><default key=\"_controller\">Fo<caret>o\\Bar</default></route></routes>",
            PlatformPatterns.psiElement().withName("Bar")
        );
    }
}
