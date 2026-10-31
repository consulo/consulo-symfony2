package fr.adrienbrault.idea.symfony2plugin.markdown;

import com.jetbrains.twig.TwigLanguage;
import consulo.language.InjectableLanguage;
import consulo.language.Language;

/**
 * Twig in a Markdown code fence: a Twig template is a template language, which needs its own file to be injected
 */
public class MarkdownTwigLanguage extends Language implements InjectableLanguage {
    public static final MarkdownTwigLanguage INSTANCE = new MarkdownTwigLanguage();

    private MarkdownTwigLanguage() {
        super(TwigLanguage.INSTANCE, "SymfonyMarkdownTwig");
    }
}
