package fr.adrienbrault.idea.symfony2plugin.templating.util;

import consulo.html.language.HTMLLanguage;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.StandardPatterns;
import consulo.xml.language.psi.pattern.XmlPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.html.language.psi.HtmlTag;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlText;
import consulo.xml.language.psi.XmlTokenType;
import consulo.language.util.ProcessingContext;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigHtmlCompletionUtil {
    public static final PatternCondition<PsiElement> HTML_TAG_TWIG_COMPONENT_PREFIX = new PatternCondition<>("twig prefix") {
        @Override
        public boolean accepts(PsiElement psiElement, ProcessingContext context) {
            String text = psiElement.getText();
            return text.startsWith("twig:");
        }
    };

    // html inside twig: href=""
    public static PsiElementPattern.Capture<PsiElement> getHrefAttributePattern() {
        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns.xmlAttribute("href")
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    public static PsiElementPattern.Capture<PsiElement> getFormActionAttributePattern() {
        return getTagAttributePattern("form", "action");
    }

    /**
     * <foo>bar</foo>
     */
    public static PsiElementPattern.Capture<PsiElement> getTagTextPattern(String... tag) {
        return XmlPatterns
            .psiElement().withParent(
                PlatformPatterns.psiElement(XmlText.class).withParent(
                    PlatformPatterns.psiElement(HtmlTag.class).withName(tag)
                )
            )
            .inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    public static PsiElementPattern.Capture<PsiElement> getTagAttributePattern(String tag, String attribute) {
        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(
                        XmlPatterns.xmlAttribute(attribute).withParent(
                            XmlPatterns.xmlTag().withName(tag)
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    // html inside twig: <link href="#" rel="stylesheet" />
    public static PsiElementPattern.Capture<PsiElement> getAssetCssAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                        .xmlAttribute("href")
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withChild(XmlPatterns
                                .xmlAttribute("rel")
                                .withValue(
                                    StandardPatterns.string().equalTo("stylesheet")
                                )
                            )
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );

    }

    // html inside twig: <script src="#"></script>
    public static PsiElementPattern.Capture<PsiElement> getAssetJsAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                        .xmlAttribute("src")
                        .withParent(XmlPatterns
                            .xmlTag().withName("script")
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );

    }

    // html inside twig: <img src="">
    public static PsiElementPattern.Capture<PsiElement> getAssetImageAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                            .xmlAttribute("src")
                            .withParent(XmlPatterns
                                    .xmlTag().withName("img")
                            )
                    )
            ).inFile(XmlPatterns.psiFile()
                    .withName(XmlPatterns
                            .string().endsWith(".twig")
                    )
            );

    }

    /**
     * "<twig:Alert></twig:Alert>"
     */
    public static PsiElementPattern.Capture<PsiElement> getTwigNamespacePattern() {
        return PlatformPatterns.psiElement().withElementType(XmlTokenType.XML_NAME).with(HTML_TAG_TWIG_COMPONENT_PREFIX);
    }

    /**
     * <twig:block name="<caret>">
     */
    public static PsiElementPattern.Capture<PsiElement> getTwigBlockNameAttributePattern() {
        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(
                        XmlPatterns.xmlAttribute("name")
                            .withParent(
                                XmlPatterns.xmlTag().with(new PatternCondition<>("twig:block") {
                                    @Override
                                    public boolean accepts(XmlTag xmlTag, ProcessingContext context) {
                                        return isTwigBlockTag(xmlTag);
                                    }
                                })
                            )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    /**
     * Matches when inside a twig: component body (not twig:block).
     * <pre>
     * {@code <twig:Alert><caret></twig:Alert>}
     * </pre>
     * This triggers completion for inserting {@code <twig:block name="..."></twig:block>} tags.
     */
    public static PsiElementPattern.Capture<PsiElement> getTwigComponentBodyPattern() {
        return PlatformPatterns.psiElement()
            .inside(XmlPatterns.xmlTag().with(new PatternCondition<>("inside twig: component") {
                @Override
                public boolean accepts(XmlTag xmlTag, ProcessingContext context) {
                    String name = xmlTag.getName();
                    return name.startsWith("twig:") && !"twig:block".equals(name);
                }
            }))
            .inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns.string().endsWith(".twig"))
            );
    }

    public static boolean isTwigBlockTag(XmlTag tag) {
        String name = tag.getName();
        if ("twig:block".equals(name)) {
            return true;
        }

        return "block".equals(name) && "twig".equals(tag.getNamespacePrefix());
    }

    /**
     * Returns true if the given XML tag is a {@code <twig:SomeName>} component tag
     * (but NOT {@code <twig:block>}).
     */
    public static boolean isTwigComponentTag(XmlTag tag) {
        if (isTwigBlockTag(tag)) {
            return false;
        }
        String name = tag.getName();
        return name.startsWith("twig:") || "twig".equals(tag.getNamespacePrefix());
    }

    /**
     * Checks if the given Twig element is inside an HTML-syntax Twig component tag
     * ({@code <twig:ComponentName>...</twig:ComponentName>}) by inspecting the
     * HTML view via the ViewProvider.
     */
    public static boolean isInsideHtmlComponentTag(PsiElement element, PsiFile twigFile) {
        int offset = element.getTextOffset();

        PsiElement htmlElement = twigFile.getViewProvider().findElementAt(offset, HTMLLanguage.INSTANCE);
        if (htmlElement == null) {
            htmlElement = twigFile.getViewProvider().findElementAt(Math.max(0, offset - 1), HTMLLanguage.INSTANCE);
        }
        if (htmlElement == null) {
            return false;
        }

        XmlTag parentTag = PsiTreeUtil.getParentOfType(htmlElement, XmlTag.class);
        while (parentTag != null) {
            if (isTwigComponentTag(parentTag)) {
                return true;
            }
            parentTag = PsiTreeUtil.getParentOfType(parentTag, XmlTag.class);
        }

        return false;
    }
}
