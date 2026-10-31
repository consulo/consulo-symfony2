package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.Constant;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redirects Find Usages started from Twig symbols to the already resolved PHP targets.
 *
 * Examples:
 * `{{ foo.id }}` on `id` -> `Bar::getId()` / `Bar::$id`
 * `{{ foo.id }}` on `foo` -> `Bar`
 * `{{ constant('Foo\\Bar::BAZ') }}` on `BAZ` -> `Foo\Bar::BAZ`
 *
 * The upstream class also implemented `UsageTargetProvider`; in Consulo one class can only implement one extension API,
 * so that part lives in {@link TwigUsageTargetProvider}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    /**
     * Activates the custom handler only when the Twig caret resolves to a supported Twig/PHP usage identity.
     */
    @Override
    public boolean canFindUsages(PsiElement element) {
        return getTwigFindUsagesTarget(element) != null;
    }

    /**
     * Stores the effective Twig or PHP search targets for the Find Usages session.
     */
    @Override
    public @Nullable FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        TwigFindUsagesTarget target = getTwigFindUsagesTarget(element);
        return target != null ? new TwigFindUsagesHandler(target) : null;
    }

    /**
     * Resolves the Find Usages identity for one Twig caret position.
     * Extension symbols stay Twig-symbol based, while all other cases keep delegating to PHP.
     */
    static @Nullable TwigFindUsagesTarget getTwigFindUsagesTarget(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null || file.getFileType() != TwigFileType.INSTANCE) {
            return null;
        }

        List<Pair<PsiElement, List<PsiElement>>> resolvedCandidates = new ArrayList<>();
        PsiElement caretElement = getTwigFindUsagesCaretElement(element);
        List<PsiElement> candidateElements = Collections.singletonList(caretElement != null ? caretElement : element);

        for (PsiElement candidate : candidateElements) {
            TwigExtensionUsageSymbol symbol = TwigExtensionUsageUtil.INSTANCE.getTwigExtensionSymbol(candidate);
            if (symbol != null) {
                return new TwigExtensionSymbolFindUsagesTarget(candidate, symbol);
            }

            List<PsiElement> resolvedTargets = new ArrayList<>();
            for (PsiElement it : TwigUsageTargetUtil.INSTANCE.getTwigFindUsagesTargets(candidate)) {
                if (it instanceof PhpClass || it instanceof Method || it instanceof Field || it instanceof PhpEnumCase || it instanceof Constant) {
                    resolvedTargets.add(it);
                }
            }

            if (!resolvedTargets.isEmpty()) {
                resolvedCandidates.add(Pair.create(candidate, resolvedTargets));
            }
        }

        Pair<PsiElement, List<PsiElement>> max = null;
        for (Pair<PsiElement, List<PsiElement>> candidate : resolvedCandidates) {
            if (max == null || compareCandidates(max, candidate) < 0) {
                max = candidate;
            }
        }

        return max != null ? new TwigPhpFindUsagesTarget(max.getFirst(), max.getSecond()) : null;
    }

    /**
     * Upstream ordering: by text offset first, then prefer candidates that resolve to something other than a class.
     */
    private static int compareCandidates(Pair<PsiElement, List<PsiElement>> a, Pair<PsiElement, List<PsiElement>> b) {
        int result = Integer.compare(a.getFirst().getTextOffset(), b.getFirst().getTextOffset());
        if (result != 0) {
            return result;
        }

        return Integer.compare(hasNonClassTarget(a.getSecond()) ? 1 : 0, hasNonClassTarget(b.getSecond()) ? 1 : 0);
    }

    private static boolean hasNonClassTarget(List<PsiElement> targets) {
        for (PsiElement target : targets) {
            if (!(target instanceof PhpClass)) {
                return true;
            }
        }

        return false;
    }

    /**
     * When Find Usages is started from the editor, the platform still passes the whole Twig field chain.
     * Use the current caret offset to recover the concrete leaf the user clicked on.
     */
    static @Nullable PsiElement getTwigFindUsagesCaretElement(PsiElement element) {
        Project project = element.getProject();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return null;
        }

        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return null;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document == null) {
            return null;
        }

        if (document != editor.getDocument()) {
            return null;
        }

        int caretOffset = editor.getCaretModel().getOffset();
        return containingFile.findElementAt(caretOffset);
    }
}
