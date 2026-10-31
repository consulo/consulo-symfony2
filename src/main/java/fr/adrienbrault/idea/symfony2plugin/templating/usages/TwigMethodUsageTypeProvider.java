package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.UsageTarget;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProvider;
import org.jspecify.annotations.Nullable;

/**
 * Groups synthetic Twig usages under a dedicated "Twig" bucket for PHP symbol searches.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigMethodUsageTypeProvider implements UsageTypeProvider {
    private static final UsageType TWIG = new UsageType(LocalizeValue.localizeTODO("Twig"));

    /**
     * Legacy API entry point. The target-aware overload below is the one used in practice.
     */
    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        return null;
    }

    /**
     * Marks Twig hits only when the current Usage View belongs to a PHP member search.
     */
    @Override
    public @Nullable UsageType getUsageType(PsiElement element, UsageTarget[] targets) {
        if (!isTwigUsageTarget(targets)) {
            return null;
        }

        return element.getContainingFile() instanceof TwigFile ? TWIG : null;
    }

    /**
     * Restricts this provider to PHP Find Usages sessions that can produce Twig synthetic references.
     */
    private static boolean isTwigUsageTarget(UsageTarget[] targets) {
        for (UsageTarget target : targets) {
            if (!(target instanceof PsiElementUsageTarget psiElementUsageTarget)) {
                continue;
            }

            PsiElement element = psiElementUsageTarget.getElement();

            // Symbol-based Twig extension usage targets like `form_start` in `{{ form_start() }}` should stay in the Twig bucket.
            if (element.getContainingFile() instanceof TwigFile && TwigExtensionUsageUtil.INSTANCE.getTwigExtensionSymbol(element) != null) {
                return true;
            }

            // Limit Twig usage grouping to the concrete PHP symbol kinds supported by the Twig search executor.
            if (element instanceof Method || element instanceof Field || element instanceof PhpEnumCase || element instanceof PhpClass) {
                return true;
            }
        }

        return false;
    }
}
