package fr.adrienbrault.idea.symfony2plugin.markdown;

import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.markdown.injection.LanguageGuesser;
import consulo.markdown.injection.LanguageVariant;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.plugins.markdown.lang.psi.MarkdownCodeFence;

import java.util.Collection;
import java.util.List;

/**
 * {@code ```twig} fences
 */
@ExtensionImpl
public class TwigCodeFenceLanguageGuesser implements LanguageGuesser {
    private static final String TWIG_INFO_STRING = "twig";

    @Nullable
    @Override
    public Language guessLanguage(@Nonnull MarkdownCodeFence markdownCodeFence, @Nonnull String fenceLanguage) {
        return guessLanguage(fenceLanguage);
    }

    @Nullable
    @Override
    public Language guessLanguage(@Nonnull String fenceLanguage) {
        return TWIG_INFO_STRING.equalsIgnoreCase(LanguageGuesser.getLanguageName(fenceLanguage)) ? MarkdownTwigLanguage.INSTANCE : null;
    }

    @Nonnull
    @Override
    public Collection<LanguageVariant> getVariants() {
        return List.of(new LanguageVariant(TWIG_INFO_STRING, TwigFileType.INSTANCE.getIcon(), TwigLanguage.INSTANCE.getDisplayName()));
    }
}
