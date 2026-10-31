package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.usage.UsageInfo;
import consulo.xml.util.xml.model.gotosymbol.GoToSymbolProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Custom Find Usages handler for route declarations.
 * It keeps IntelliJ's search behavior but replaces the top-level target node with a presentation wrapper.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteFindUsagesHandler extends FindUsagesHandler {
    public RouteFindUsagesHandler(PsiElement psiElement) {
        super(psiElement);
    }

    /**
     * Replaces the raw declaration PSI with a navigation item that can expose icon and gray location text.
     */
    @Override
    public PsiElement[] getPrimaryElements() {
        return new PsiElement[]{new RouteFindUsagesNavigationItem(getPsiElement())};
    }

    /**
     * Unwraps the presentation wrapper back to the real declaration before searching usages.
     */
    @Override
    public boolean processElementUsages(PsiElement element, Predicate<UsageInfo> processor, FindUsagesOptions options) {
        return super.processElementUsages(unwrapRouteFindUsagesElement(element), processor, options);
    }

    /**
     * Applies the same unwrap logic for text usage searching.
     */
    @Override
    public boolean processUsagesInText(PsiElement element, Predicate<UsageInfo> processor, GlobalSearchScope searchScope) {
        return super.processUsagesInText(unwrapRouteFindUsagesElement(element), processor, searchScope);
    }

    /**
     * Converts the synthetic navigation item back into the underlying declaration PSI.
     */
    private static PsiElement unwrapRouteFindUsagesElement(PsiElement element) {
        if (element instanceof RouteFindUsagesNavigationItem) {
            return ((RouteFindUsagesNavigationItem) element).getDelegate();
        }
        else {
            return element;
        }
    }

    /**
     * Presentation-only wrapper for the top Find Usages node of a route declaration.
     * The delegate remains the real PSI target used by the actual search.
     */
    public static class RouteFindUsagesNavigationItem extends GoToSymbolProvider.BaseNavigationItem {
        private final PsiElement myDelegate;

        public RouteFindUsagesNavigationItem(PsiElement delegate) {
            super(delegate, "", null);
            myDelegate = delegate;
        }

        public PsiElement getDelegate() {
            return myDelegate;
        }

        @Override
        public Image getIcon() {
            return Symfony2Icons.ROUTE;
        }

        /**
         * Supplies the UI-facing presentation used by the Usage View header row.
         */
        @Override
        public ItemPresentation getPresentation() {
            return new ItemPresentation() {
                @Override
                public LocalizeValue getPresentableText() {
                    return LocalizeValue.of(RouteUsageUtil.getPresentableText(myDelegate));
                }

                @Override
                public @Nullable String getLocationString() {
                    return RouteUsageUtil.getLocationString(myDelegate);
                }

                @Override
                public Image getIcon() {
                    return RouteFindUsagesNavigationItem.this.getIcon();
                }
            };
        }
    }
}
