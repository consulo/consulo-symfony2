package fr.adrienbrault.idea.symfony2plugin.markdown;

import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.markdown.injection.LanguageGuesser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl;

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
    public Language guessLanguage(@Nonnull MarkdownCodeFenceImpl markdownCodeFence, @Nonnull String fenceLanguage) {
        return guessLanguage(fenceLanguage);
    }

    @Nullable
    @Override
    public Language guessLanguage(@Nonnull String fenceLanguage) {
        return TWIG_INFO_STRING.equalsIgnoreCase(LanguageGuesser.getLanguageName(fenceLanguage)) ? MarkdownTwigLanguage.INSTANCE : null;
    }

    @Nonnull
    @Override
    public Collection<LookupElement> getCompletionVariants(@Nonnull CompletionParameters parameters) {
        return List.of(LookupElementBuilder.create(TWIG_INFO_STRING)
            .withIcon(TwigFileType.INSTANCE.getIcon())
            .withTypeText(TwigLanguage.INSTANCE.getDisplayName().get(), true));
    }
}
