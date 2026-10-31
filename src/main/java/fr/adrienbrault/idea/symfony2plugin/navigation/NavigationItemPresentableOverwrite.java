package fr.adrienbrault.idea.symfony2plugin.navigation;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNavigationSupport;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NavigationItemPresentableOverwrite implements NavigationItem, ItemPresentation {

    private final PsiElement psiElement;

    private final String presentableText;

    private final Image icon;

    private final String locationString;
    private final String name;

    private NavigationItemPresentableOverwrite(PsiElement psiElement, String presentableText, Image icon, String locationString, String name) {
        this.psiElement = psiElement;
        this.presentableText = presentableText;
        this.name = name;
        this.icon = icon;
        this.locationString = locationString;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public LocalizeValue getPresentableText() {
                return LocalizeValue.of(NavigationItemPresentableOverwrite.this.presentableText);
            }

            @Override
            public String getLocationString() {
                return NavigationItemPresentableOverwrite.this.locationString;
            }

            @Override
            public @Nullable Image getIcon(boolean unused) {
                return NavigationItemPresentableOverwrite.this.getIcon(unused);
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this.psiElement);
        if (descriptor != null) {
            descriptor.navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this.psiElement);
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public String toString() {
        return this.presentableText;
    }

    @Override
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(presentableText);
    }

    @Override
    public String getLocationString() {
        return this.locationString;
    }
    
    @Nullable
    @Override
    public Image getIcon(boolean b) {
        return icon;
    }

    public static NavigationItemPresentableOverwrite create(PsiElement psiElement, String presentableText, Image icon, String locationString, boolean appendBundleLocation, String name) {
        String locationPathString = locationString;

        if (appendBundleLocation) {
            PsiFile psiFile = psiElement.getContainingFile();
            if (psiFile != null) {
                locationPathString = NavigationItemEx.buildBundleLocationString(locationString, psiFile);
            }
        }

        return new NavigationItemPresentableOverwrite(
            psiElement,
            presentableText,
            icon,
            locationPathString,
            name
        );
    }
}

