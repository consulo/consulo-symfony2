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
 * "Search everywhere" does instance check on NavigationItem, but ChooseByNameContributor use getPresentation :)
 * Moreover "Search everywhere" dont set icons at all
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NavigationItemEx implements NavigationItem, ItemPresentation {

    private final PsiElement psiElement;
    private final String name;
    private final Image icon;
    private final String locationString;
    private boolean appendBundleLocation = true;

    public NavigationItemEx(PsiElement psiElement, final String name, final Image icon, final String locationString) {
        this.psiElement = psiElement;
        this.name = name;
        this.icon = icon;
        this.locationString = locationString;
    }

    public NavigationItemEx(PsiElement psiElement, final String name, final Image icon, final String locationString, boolean appendBundleLocation) {
        this(psiElement, name, icon, locationString);
        this.appendBundleLocation = appendBundleLocation;
    }

    @Nullable
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
        return LocalizeValue.ofNullable(name);
    }

    @Nullable
    @Override
    public String getLocationString() {
        if(!this.appendBundleLocation) {
            return this.locationString;
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if(psiFile == null) {
            return this.locationString;
        }

        String locationPathString = this.locationString;

        String bundleName = psiFile.getVirtualFile().getPath();

        if(bundleName.contains("Bundle")) {
            bundleName = bundleName.substring(0, bundleName.lastIndexOf("Bundle"));
            if(bundleName.length() > 1 && bundleName.contains("/")) {
                return locationPathString + " " + bundleName.substring(bundleName.lastIndexOf("/") + 1) + "::" + psiFile.getName();
            }
        }

        return locationPathString + " " + psiFile.getName();
    }

    @Nullable
    @Override
    public Image getIcon(boolean b) {
        return icon;
    }

    /**
     * Appends bundle and file name to the location string when the file path contains "Bundle".
     * Format: {@code "locationString BundleDir::filename"}, or {@code "locationString filename"} as fallback.
     */
    public static String buildBundleLocationString(String locationString, PsiFile psiFile) {
        String locationPathString = locationString + " " + psiFile.getName();

        String bundlePath = psiFile.getVirtualFile().getPath();
        if (bundlePath.contains("Bundle")) {
            bundlePath = bundlePath.substring(0, bundlePath.lastIndexOf("Bundle"));
            if (bundlePath.length() > 1 && bundlePath.contains("/")) {
                locationPathString = locationPathString + " " + bundlePath.substring(bundlePath.lastIndexOf("/") + 1) + "::" + psiFile.getName();
            }
        }

        return locationPathString;
    }

}

