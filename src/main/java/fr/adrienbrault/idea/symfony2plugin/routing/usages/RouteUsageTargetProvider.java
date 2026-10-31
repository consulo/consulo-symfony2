package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.usage.PsiElementUsageTargetFactory;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the initial UsageTarget for route Find Usages.
 * This is the common entry point for both declaration-side searches and searches started from route usage strings.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteUsageTargetProvider implements UsageTargetProvider {
    /**
     * Resolves the caret to the owning route declaration target, regardless of whether the caret is on the declaration itself
     * or on a PHP/Twig route usage string.
     */
    @Override
    public UsageTarget[] getTargets(PsiElement element) {
        List<UsageTarget> targets = new ArrayList<>();
        for (PsiElement target : RouteUsageUtil.getRouteSearchTargets(element)) {
            // consulo-port: upstream PsiElement2UsageTargetAdapter(it, true); the highlightUsages flag has no counterpart
            targets.add(PsiElementUsageTargetFactory.getInstance().create(target));
        }

        UsageTarget[] usageTargets = targets.toArray(UsageTarget.EMPTY_ARRAY);

        return usageTargets.length == 0 ? UsageTarget.EMPTY_ARRAY : usageTargets;
    }

    /**
     * Editor-based entry point required by the platform. It forwards the caret PSI to the PSI-based overload.
     */
    @Override
    public UsageTarget @Nullable [] getTargets(Editor editor, PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret == null) {
            return null;
        }

        UsageTarget[] targets = getTargets(elementAtCaret);
        return targets.length > 0 ? targets : null;
    }
}
