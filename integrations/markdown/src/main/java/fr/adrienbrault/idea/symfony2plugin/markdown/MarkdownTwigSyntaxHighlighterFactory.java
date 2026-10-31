package fr.adrienbrault.idea.symfony2plugin.markdown;

import com.jetbrains.twig.TwigHighlighter;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class MarkdownTwigSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @Nonnull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return new TwigHighlighter.TwigFileHighlighter(project);
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return MarkdownTwigLanguage.INSTANCE;
    }
}
