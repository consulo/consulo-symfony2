package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import org.junit.jupiter.api.Test;

import consulo.language.editor.inlay.InlayInfo;
import consulo.xml.language.XmlFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceArgumentParameterHintsProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see ServiceArgumentParameterHintsProvider
 */
public class ServiceArgumentParameterHintsProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ServiceArgumentParameterHintsProvider.php");
        myFixture.copyFileToProject("ServiceArgumentParameterHintsProvider.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/fixtures";
    }

    @Test

    public void testXmlParameterTypeHintForIdAttribute() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument type=\"service\" id=\"a<caret>a\">\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeHintForXmlText() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument>%a<caret>a%</argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeHintWithoutTypeHintMustFallbackToParameterName() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument>a</argument>\n" +
            "           <argument>%a<caret>a%</argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "foobar".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeHintWithoutTypeHintMustFallbackToParameterNameOnClassAsId() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"Foobar\\MyFoobar\">\n" +
            "           <argument>a</argument>\n" +
            "           <argument>%a<caret>a%</argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "foobar".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeHintForNamedArguments() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"Foobar\\MyFoobarNamed\">\n" +
            "           <argument key=\"$foo\" id=\"a<caret>a\"/>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeForNestedArgumentsMustNotProvideHint() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument type=\"collection\">\n" +
            "               <caret>\n" +
            "               <argument/>" +
            "           </argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertSize(0, parameterHints);
    }

    @Test

    public void testXmlParameterTypeForId() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"my_<caret>foo\" alias=\"foo_alias_hint\"/>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "MyFoobar".equals(inlayInfo.getText())));
    }

    @Test

    public void testXmlParameterTypeHintForCallArgument() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "            <call method=\"setFoo\">\n" +
            "                <argument type=\"service\" id=\"ma<caret>iler\" />\n" +
            "            </call>" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testYamlParameterTypeForId() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    foobar:\n" +
            "        class: Foobar\\MyFoobar\n" +
            "        arguments: [@fo<caret>obar]\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testYamlParameterTypeForServiceIdShortcut() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    Foobar\\MyFoobar:\n" +
            "        arguments: [@fo<caret>obar]\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testYamlParameterTypeForServiceIdShortcutWithNamedArgument() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    Foobar\\MyFoobar:\n" +
            "        arguments:\n" +
            "           $foo: @fo<caret>obar\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testYamlParameterTypeForCallMethodParameter() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    foobar:\n" +
            "        class: Foobar\\MyFoobar\n" +
            "        calls:\n" +
            "           - [setFoo, [@fo<caret>obar]]\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @Test

    public void testYamlParameterTypeForCallMethodParameterWithIdShortcut() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    Foobar\\MyFoobar:\n" +
            "        calls:\n" +
            "           - [setFoo, [@fo<caret>obar]]\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    private List<InlayInfo> getInlayInfo(FileType fileType, String content) {
        myFixture.configureByText(fileType, content);

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        PsiElement parent = psiElement.getParent();

        return new ServiceArgumentParameterHintsProvider().getParameterHints(parent);
    }

    private List<InlayInfo> getInlayInfo(String content) {
        return getInlayInfo(XmlFileType.INSTANCE, content);
    }
}
