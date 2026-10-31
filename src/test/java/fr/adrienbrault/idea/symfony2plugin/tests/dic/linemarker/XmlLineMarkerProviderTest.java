package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import org.junit.jupiter.api.Test;

import consulo.xml.language.XmlFileType;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.xml.language.psi.pattern.XmlPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.linemarker.XmlLineMarkerProvider
 */
public class XmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.xml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/linemarker/fixtures";
    }

    @Test

    public void testThatDecoratedServiceShouldProvideMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" class=\"Foo\\Bar\\Apple\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decoration")
        );
    }

    @Test

    public void testThatParentServiceShouldProvideMarker() {
        myFixture.addFileToProject("config/services_parent.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "       <service id=\"app.child\" parent=\"foo_bar_parent_main_xml\"/>\n" +
            "    </services>\n" +
            "</container>");

        PsiElement xmlFile = createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "       <service id=\"foo_bar_parent_main_xml\" class=\"Foo\\Bar\\Apple\"/>\n" +
            "    </services>\n" +
            "</container>"
        );

        assertLineMarker(xmlFile, new LineMarker.ToolTipEqualsAssert("Navigate to parent"));
        assertLineMarker(xmlFile, new LineMarker.TargetAcceptsPattern("Navigate to parent",
            XmlPatterns.xmlTag().withName("service").withAttributeValue("id", "app.child"))
        );
    }

    @Test

    public void testThatDecoratesProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" decorates=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to decorated service")
        );
    }

    @Test

    public void testThatParentProvidesOverwriteMarker() {
        assertLineMarker(createXmlFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "       <service id=\"foo_bar_main\" parent=\"app.mailer\"/>\n" +
                "    </services>\n" +
                "</container>"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to parent service")
        );
    }

    @Test

    public void testThatPrototypeNamespaceResourceIsHavingLinemarker() {
        myFixture.addFileToProject("src/Controller/FooController.php", "<?php\n" +
            "namespace App\\Controller;\n" +
            "class FooController {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.xml",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "        <prototype namespace=\"App\\Controller\\\" resource=\"../src/Controller/*\"/>\n" +
            "    </services>\n" +
            "</container>");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(myFixture.getFile(), new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Controller\\FooController".equals(phpClass.getFQN());
                }
            }))
        );
    }

    private PsiElement createXmlFile(String content) {
        return PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, content);
    }
}
