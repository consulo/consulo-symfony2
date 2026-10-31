package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.PhpFile;
import consulo.component.util.Iconable;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.util.xml.model.gotosymbol.GoToSymbolProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyTargetFakePsiNavigationItem extends GoToSymbolProvider.BaseNavigationItem {
    private final VirtualFile projectDir;
    private final PsiElement myPsiElement;
    private final String rootDir;

    public TranslationKeyTargetFakePsiNavigationItem(VirtualFile projectDir, PsiElement psiElement) {
        super(psiElement, "", null);

        this.projectDir = projectDir;
        this.rootDir = VirtualFileUtil.getRelativePath(psiElement.getContainingFile().getVirtualFile(), projectDir, '/');
        this.myPsiElement = psiElement;
    }

    // consulo-port: upstream overrides ItemPresentation#getIcon(boolean); Consulo's primary method is getIcon()
    @Override
    public Image getIcon() {
        return getFileIcon();
    }

    public int getWeight() {
        if (("/" + rootDir).startsWith("/translations/")) {
            return -1;
        }

        return isSymfonyTranslationDirectory() ? 1 : 0;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public LocalizeValue getPresentableText() {
                return LocalizeValue.of(myPsiElement.getContainingFile().getName());
            }

            @Override
            public String getLocationString() {
                String relativePath = VirtualFileUtil.getRelativePath(myPsiElement.getContainingFile().getVirtualFile(), projectDir, '/');
                if (relativePath != null) {
                    return '(' + relativePath + ')';
                }

                return '(' + myPsiElement.getContainingFile().getVirtualFile().toString() + ')';
            }

            @Override
            public @Nullable Image getIcon() {
                // consulo-port: upstream getIcon(ICON_FLAG_VISIBILITY); PsiElement has no getIcon(int) in Consulo
                return TranslationKeyTargetFakePsiNavigationItem.this.getIcon();
            }
        };
    }

    private Image getFileIcon() {
        Image fileIcon;

        PsiFile containingFile = myPsiElement.getContainingFile();
        if (containingFile instanceof YAMLFile) {
            fileIcon = YAMLFileType.YML.getIcon();
        } else if (containingFile instanceof PhpFile) {
            fileIcon =  PhpIcons.PhpIcon;
        } else if (containingFile instanceof XmlFile) {
            fileIcon =  PlatformIconGroup.filetypesXml();
        } else if (containingFile instanceof JsonFile) {
            fileIcon =  JsonFileType.INSTANCE.getIcon();
        } else {
            // for non registered state
            String extension = myPsiElement.getContainingFile().getVirtualFile().getExtension();

            fileIcon = "xlf".equalsIgnoreCase(extension) || "xliff".equalsIgnoreCase(extension)
                ? PlatformIconGroup.filetypesXml()
                : IconDescriptorUpdaters.getIcon(myPsiElement, Iconable.ICON_FLAG_VISIBILITY);
        }

        if (rootDir != null && isSymfonyTranslationDirectory()) {
            Image sizedIcon = ImageEffects.resize(Symfony2Icons.SYMFONY_LINE_MARKER, fileIcon.getWidth() / 2, fileIcon.getHeight() / 2);
            // TODO consulo-port: LayeredIcon#setIcon(sizedIcon, 1, SwingConstants.SOUTH_EAST) has no counterpart; ImageEffects.layered draws the overlay at the top-left
            return ImageEffects.layered(fileIcon, sizedIcon);
        }

        return fileIcon;
    }


    private boolean isSymfonyTranslationDirectory() {
        return ("/" + rootDir).contains("/vendor/") && rootDir.contains("/symfony/") && rootDir.contains("/Resources/translations/");
    }
}
