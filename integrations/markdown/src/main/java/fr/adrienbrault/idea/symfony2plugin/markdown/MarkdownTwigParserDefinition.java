package fr.adrienbrault.idea.symfony2plugin.markdown;

import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.parser.TwigParserDefinition;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class MarkdownTwigParserDefinition extends TwigParserDefinition {
    private static final IFileElementType FILE = new IFileElementType("SYMFONY_MARKDOWN_TWIG_FILE", MarkdownTwigLanguage.INSTANCE);

    @Override
    public Language getLanguage() {
        return MarkdownTwigLanguage.INSTANCE;
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new MarkdownTwigFile(viewProvider);
    }

    private static class MarkdownTwigFile extends PsiFileBase {
        private MarkdownTwigFile(FileViewProvider viewProvider) {
            super(viewProvider, MarkdownTwigLanguage.INSTANCE);
        }

        @Nonnull
        @Override
        public FileType getFileType() {
            return TwigFileType.INSTANCE;
        }

        // the view provider is the one of the Markdown host; resolve against the injected Twig tree instead
        @Nullable
        @Override
        @RequiredReadAction
        public PsiElement findElementAt(int offset) {
            ASTNode leaf = getNode().findLeafElementAt(offset);
            return leaf == null ? null : leaf.getPsi();
        }

        @Override
        public String toString() {
            return "Markdown Twig file";
        }
    }
}
