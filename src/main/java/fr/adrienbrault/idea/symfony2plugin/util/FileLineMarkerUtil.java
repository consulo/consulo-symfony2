package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Centralizes line-marker creation for elements that may be whole files.
 * File markers are anchored to the first non-blank leaf and clipped to one line,
 * so unified diff views do not repeat one file-wide marker in every changed block.
 */
public final class FileLineMarkerUtil {
    private FileLineMarkerUtil() {
    }

    /**
     * Creates a builder-based marker for a file anchor.
     * Example: {@code FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile)}.
     */
    public static RelatedItemLineMarkerInfo<PsiElement> createLineMarkerInfo(
        NavigationGutterIconBuilder<PsiElement> builder,
        PsiFile psiFile
    ) {
        LineMarkerAnchor anchor = getFileLineMarkerAnchor(psiFile);
        PsiElement target = anchor.element();
        RelatedItemLineMarkerInfo<PsiElement> markerInfo = builder.createLineMarkerInfo(target);

        TextRange range = anchor.textRange();
        if (range.equals(target.getTextRange())) {
            return markerInfo;
        }

        String tooltip = markerInfo.getLineMarkerTooltip();
        return new RelatedItemLineMarkerInfo<>(
            target,
            range,
            // consulo-port: LineMarkerInfo has no public getIcon(); the builder always sets an icon
            markerInfo.createGutterRenderer().getIcon(),
            Pass.LINE_MARKERS,
            tooltip == null ? null : element -> tooltip,
            markerInfo.getNavigationHandler(),
            GutterIconRenderer.Alignment.RIGHT,
            markerInfo::createGotoRelatedItems
        );
    }

    /**
     * Creates a direct marker using the same anchoring rules as builder markers.
     * Example: {@code FileLineMarkerUtil.createLineMarkerInfo(target, icon, tooltip, handler, () -> "Go to related files")}.
     */
    public static LineMarkerInfo<PsiElement> createLineMarkerInfo(
        PsiElement lineMarkerTarget,
        Image icon,
        @Nullable String tooltip,
        @Nullable GutterIconNavigationHandler<PsiElement> navigationHandler,
        Supplier<String> accessibleNameProvider
    ) {
        LineMarkerAnchor anchor = getLineMarkerAnchor(lineMarkerTarget);

        return new LineMarkerInfo<>(
            anchor.element(),
            anchor.textRange(),
            icon,
            Pass.LINE_MARKERS,
            tooltip == null ? null : element -> tooltip,
            navigationHandler,
            GutterIconRenderer.Alignment.RIGHT
            // consulo-port: LineMarkerInfo has no accessible name provider; accessibleNameProvider is unused
        );
    }

    private static LineMarkerAnchor getLineMarkerAnchor(PsiElement element) {
        if (element instanceof PsiFile psiFile) {
            return getFileLineMarkerAnchor(psiFile);
        }

        return new LineMarkerAnchor(element, element.getTextRange());
    }

    /**
     * Returns the file anchor used by {@link #createLineMarkerInfo(NavigationGutterIconBuilder, PsiFile)}.
     * Useful in tests that need to assert the exact marker element and range.
     */
    public static LineMarkerAnchor getFileLineMarkerAnchor(PsiFile psiFile) {
        PsiElement target = getFileLineMarkerTarget(psiFile);
        return new LineMarkerAnchor(target, getFileLineMarkerRange(psiFile, target));
    }

    private static PsiElement getFileLineMarkerTarget(PsiFile psiFile) {
        for (PsiElement leaf = PsiTreeUtil.getDeepestFirst(psiFile); leaf != null && leaf != psiFile; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (leaf.getTextLength() > 0 && !StringUtil.isEmptyOrSpaces(leaf.getText())) {
                return leaf;
            }
        }

        return psiFile;
    }

    private static TextRange getFileLineMarkerRange(PsiFile psiFile, PsiElement target) {
        TextRange targetRange = target.getTextRange();
        int startOffset = targetRange.getStartOffset();
        int endOffset = targetRange.getEndOffset();

        Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (document == null) {
            return targetRange;
        }

        int textLength = document.getTextLength();
        if (startOffset >= textLength) {
            return TextRange.create(startOffset, startOffset);
        }

        int lineEndOffset = document.getLineEndOffset(document.getLineNumber(startOffset));
        int clippedEndOffset = Math.min(endOffset, lineEndOffset);
        if (clippedEndOffset <= startOffset) {
            clippedEndOffset = Math.min(startOffset + 1, textLength);
        }

        return TextRange.create(startOffset, clippedEndOffset);
    }

    /**
     * The PSI element and text range that should receive the gutter marker.
     */
    public record LineMarkerAnchor(PsiElement element, TextRange textRange) {
    }
}
