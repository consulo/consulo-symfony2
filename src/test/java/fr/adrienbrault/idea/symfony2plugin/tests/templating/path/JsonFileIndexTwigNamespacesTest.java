package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import org.junit.jupiter.api.Test;

import consulo.util.lang.function.Condition;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

public class JsonFileIndexTwigNamespacesTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("ide-twig.json", "foo/ide-twig.json");
        myFixture.copyFileToProject("test.html.twig", "foo/res/test.html.twig");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/fixtures";
    }

    @Test

    public void testThatNamespaceAndPathIsAddedToNamespaceList() {

        List<TwigPath> twigNamespaces = TwigUtil.getTwigNamespaces(getProject());
        TwigPath foo = ContainerUtil.find(twigNamespaces, new MyTwigPathNamespaceCondition("foo"));

        assertNotNull(foo);
        assertEquals("src/foo/res", foo.getPath());
        assertEquals("foo", foo.getNamespace());
        assertEquals(true, foo.isEnabled());
        assertEquals(TwigUtil.NamespaceType.ADD_PATH, foo.getNamespaceType());
        assertEquals(true, foo.isCustomPath());
    }

    @Test

    public void testThatPathValueIsNormalized() {
        List<TwigPath> twigNamespaces = TwigUtil.getTwigNamespaces(getProject());
        assertEquals("src/foo/res", ContainerUtil.find(twigNamespaces, new MyTwigPathNamespaceCondition("foo1")).getPath());
        assertEquals("src/foo", ContainerUtil.find(twigNamespaces, new MyTwigPathNamespaceCondition("bar")).getPath());
    }

    @Test

    public void testThatBundleNamespaceIsSupported() {
        TwigPath fooBundle = ContainerUtil.find(TwigUtil.getTwigNamespaces(getProject()), new MyTwigPathNamespaceCondition("FooBundle"));
        assertNotNull(fooBundle);
        assertEquals("src/foo/res", fooBundle.getPath());
        assertEquals("FooBundle", fooBundle.getNamespace());
        assertEquals(TwigUtil.NamespaceType.BUNDLE, fooBundle.getNamespaceType());
    }

    private static class MyTwigPathNamespaceCondition implements Condition<TwigPath> {

        private final String namespace;

        public MyTwigPathNamespaceCondition(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public boolean value(TwigPath twigPath) {
            return this.namespace.equals(twigPath.getNamespace());
        }
    }
}
