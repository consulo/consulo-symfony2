package fr.adrienbrault.idea.symfony2plugin.documentation;

import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.dataContext.DataContext;
import consulo.find.FindManager;
import consulo.language.psi.PsiElement;
import consulo.ui.ModalityState;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * Opens Find Usages from the "Find usages" links of the route and Twig template documentation
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class DocumentationFindUsagesLinkHandler {
    private DocumentationFindUsagesLinkHandler() {
    }

    /**
     * @param contextProvider computes the find usages context in a read action, with the {@link UsageView#USAGE_TARGETS_KEY} targets
     */
    public static void findUsages(Callable<@Nullable DataContext> contextProvider) {
        ReadAction.nonBlocking(contextProvider)
            .finishOnUiThread(app -> ModalityState.nonModal(), context -> {
                UsageTarget[] targets = context == null ? null : context.getData(UsageView.USAGE_TARGETS_KEY);
                if (targets == null || targets.length == 0 || !targets[0].isValid()) {
                    return;
                }

                // a PSI target searches with the options of the find usages handler of its element, like the Find Usages action;
                // its own findUsages() takes default options, which leave out the references
                if (targets[0] instanceof PsiElementUsageTarget psiTarget && psiTarget.getElement() != null) {
                    PsiElement element = psiTarget.getElement();
                    FindManager.getInstance(element.getProject()).findUsages(element);
                }
                else {
                    targets[0].findUsages();
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }
}
