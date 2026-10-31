package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import com.jetbrains.php.lang.psi.PhpFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.UsageTarget;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndexKt;
import org.jspecify.annotations.Nullable;

/**
 * Groups route usages in the Usage View into the user-facing buckets shown in the tree.
 * It only activates when the current Find Usages target is a route declaration.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteUsageTypeProvider implements UsageTypeProvider {
    private static final UsageType TWIG = new UsageType(LocalizeValue.localizeTODO("Twig"));
    private static final UsageType PHP = new UsageType(LocalizeValue.localizeTODO("PHP"));

    /**
     * Legacy API entry point. IntelliJ prefers the target-aware overload below.
     */
    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        return null;
    }

    /**
     * Classifies each route usage only when the current search target is a route declaration.
     */
    @Override
    public @Nullable UsageType getUsageType(PsiElement element, UsageTarget[] targets) {
        if (!isRouteDeclarationTargetForUsageType(targets)) {
            return null;
        }

        if (element.getContainingFile() instanceof PhpFile) {
            return PHP;
        }

        return TwigRouteUsageStubIndexKt.getTwigRouteUsageKind(element) != null ? TWIG : null;
    }

    /**
     * Confirms that the current Usage View really belongs to a route declaration target.
     */
    private static boolean isRouteDeclarationTargetForUsageType(UsageTarget[] targets) {
        for (UsageTarget target : targets) {
            PsiElement element = getRouteUsageTargetElement(target);
            if (element != null && RouteUsageUtil.getRouteNameForDeclaration(element) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Unwraps synthetic usage targets back to the declaration PSI used by the search.
     */
    private static @Nullable PsiElement getRouteUsageTargetElement(UsageTarget target) {
        if (!(target instanceof PsiElementUsageTarget)) {
            return null;
        }

        PsiElement element = ((PsiElementUsageTarget) target).getElement();
        if (element instanceof RouteFindUsagesHandler.RouteFindUsagesNavigationItem) {
            return ((RouteFindUsagesHandler.RouteFindUsagesNavigationItem) element).getDelegate();
        }
        else {
            return element;
        }
    }
}
