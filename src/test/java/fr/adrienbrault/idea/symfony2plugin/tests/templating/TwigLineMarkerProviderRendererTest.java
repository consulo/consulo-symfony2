package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import org.junit.jupiter.api.Test;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlTag;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer
 */
public class TwigLineMarkerProviderRendererTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Test
    public void testRendersTwigComponentTagUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "<twig:Alert />");

        XmlTag xmlTag = null;
        for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
            xmlTag = PsiTreeUtil.findChildOfType(psiFile, XmlTag.class);
            if (xmlTag != null) {
                break;
            }
        }

        assertNotNull(xmlTag);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert", renderer.getElementText(xmlTag));
    }

    @Test

    public void testRendersComponentFunctionUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{{ component('Card') }}");
        PsiElement stringElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, TwigPattern.getComponentPattern()::accepts))
            .findFirst()
            .orElse(null);
        assertNotNull(stringElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Card", renderer.getElementText(stringElement));
    }

    @Test

    public void testRendersComponentTagBlockUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{% component Banner with {type: 'success'} %}{% endcomponent %}");
        PsiElement componentElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, psiElement ->
                psiElement.getNode() != null &&
                    psiElement.getNode().getElementType() == TwigTokenTypes.IDENTIFIER &&
                    "Banner".equals(psiElement.getText())
            ))
            .findFirst()
            .orElse(null);

        assertNotNull(componentElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Banner", renderer.getElementText(componentElement));
    }

    @Test

    public void testRendersNestedComponentNameWithTagSyntax() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "<twig:Alert:Html:Foo_Bar_1 />");

        XmlTag xmlTag = null;
        for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
            xmlTag = PsiTreeUtil.findChildOfType(psiFile, XmlTag.class);
            if (xmlTag != null) {
                break;
            }
        }

        assertNotNull(xmlTag);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert:Html:Foo_Bar_1", renderer.getElementText(xmlTag));
    }

    @Test

    public void testRendersNestedComponentNameWithFunctionSyntax() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{{ component('Alert:Html:Foo_Bar_1') }}");
        PsiElement stringElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, TwigPattern.getComponentPattern()::accepts))
            .findFirst()
            .orElse(null);
        assertNotNull(stringElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert:Html:Foo_Bar_1", renderer.getElementText(stringElement));
    }
}
