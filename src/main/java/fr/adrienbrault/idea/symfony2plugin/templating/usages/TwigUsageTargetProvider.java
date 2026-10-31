package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.usage.PsiElementUsageTargetFactory;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Allows Find Usages to start directly on Twig usage PSI by exposing either Twig symbol targets
 * or delegated PHP targets depending on the resolved usage identity.
 *
 * Split out of {@link TwigFindUsagesHandlerFactory}: upstream registered one class for both
 * `findUsagesHandlerFactory` and `usageTargetProvider`.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigUsageTargetProvider implements UsageTargetProvider {
    /**
     * Allows Find Usages to start directly on Twig usage PSI by exposing either Twig symbol targets
     * or delegated PHP targets depending on the resolved usage identity.
     */
    @Override
    public UsageTarget[] getTargets(PsiElement element) {
        TwigFindUsagesTarget target = TwigFindUsagesHandlerFactory.getTwigFindUsagesTarget(element);
        if (target == null) {
            return UsageTarget.EMPTY_ARRAY;
        }

        List<PsiElement> primaryElements = target.getPrimaryElements();
        UsageTarget[] usageTargets = new UsageTarget[primaryElements.size()];
        for (int i = 0; i < primaryElements.size(); i++) {
            // upstream: new PsiElement2UsageTargetAdapter(it, true); the highlightUsages flag has no Consulo API counterpart
            usageTargets[i] = PsiElementUsageTargetFactory.getInstance().create(primaryElements.get(i));
        }

        return usageTargets.length == 0 ? UsageTarget.EMPTY_ARRAY : usageTargets;
    }

    /**
     * Editor-based entry point required by the platform. It forwards the caret PSI to the PSI-based overload.
     */
    @Override
    public @Nullable UsageTarget[] getTargets(Editor editor, PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret == null) {
            return null;
        }

        UsageTarget[] targets = getTargets(elementAtCaret);
        return targets.length > 0 ? targets : null;
    }
}
