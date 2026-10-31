package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.component.util.Iconable;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.xml.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a block PSI element for the goto-declaration popover when multiple targets exist,
 * enriching each entry with a project-relative file path so the user can distinguish
 * blocks with the same name defined in different templates.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockFakePsiNavigationItem extends GoToSymbolProvider.BaseNavigationItem {
    private final @Nullable VirtualFile projectDir;
    private final PsiElement myPsiElement;

    public TwigBlockFakePsiNavigationItem(@Nullable VirtualFile projectDir, PsiElement psiElement) {
        super(psiElement, "", null);

        this.projectDir = projectDir;
        this.myPsiElement = psiElement;
    }

    // consulo-port: was getIcon(boolean) returning PsiFile#getIcon(int); Consulo has getIcon() and IconDescriptorUpdaters
    @Override
    public @Nullable Image getIcon() {
        return IconDescriptorUpdaters.getIcon(myPsiElement.getContainingFile(), Iconable.ICON_FLAG_VISIBILITY);
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public LocalizeValue getPresentableText() {
                return LocalizeValue.of(myPsiElement.getText());
            }

            @Override
            public String getLocationString() {
                if (projectDir != null) {
                    String relativePath = VirtualFileUtil.getRelativePath(myPsiElement.getContainingFile().getVirtualFile(), projectDir, '/');
                    if (relativePath != null) {
                        return '(' + relativePath + ')';
                    }
                }


                return '(' + myPsiElement.getContainingFile().getVirtualFile().toString() + ')';
            }

            @Override
            @Nullable
            public Image getIcon() {
                return TwigBlockFakePsiNavigationItem.this.getIcon();
            }
        };
    }
}
