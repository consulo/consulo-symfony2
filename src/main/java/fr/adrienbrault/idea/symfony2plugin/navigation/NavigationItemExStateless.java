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
public class NavigationItemExStateless implements NavigationItem, ItemPresentation {

    private final PsiElement psiElement;

    private final String name;

    private final Image icon;

    private final String locationString;

    private NavigationItemExStateless(PsiElement psiElement, String name, Image icon, String locationString) {
        this.psiElement = psiElement;
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
        return this;
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
        return this.name;
    }

    @Override
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(name);
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

    public static NavigationItemExStateless create(PsiElement psiElement, String name, Image icon, String locationString, boolean appendBundleLocation) {
        String locationPathString = locationString;

        if (appendBundleLocation) {
            PsiFile psiFile = psiElement.getContainingFile();
            if (psiFile != null) {
                locationPathString = NavigationItemEx.buildBundleLocationString(locationString, psiFile);
            }
        }

        return new NavigationItemExStateless(
            psiElement,
            name,
            icon,
            locationPathString
        );
    }
}

