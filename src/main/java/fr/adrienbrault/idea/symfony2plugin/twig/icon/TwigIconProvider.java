package fr.adrienbrault.idea.symfony2plugin.twig.icon;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import com.jetbrains.twig.icons.TwigIcons;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Based on Twig content add overlay to the default Twig file icon, indicating the possible template type.
 *
 * Icon positions:
 * - Controller: top left (NORTH_WEST)
 * - Extends: bottom left (SOUTH_WEST)
 * - Include: bottom right (SOUTH_EAST)
 *
 * All 7 possible badge combinations are pre-built as file-level instances (bitmask index 1–7)
 * to avoid repeated layered icon instantiation on every file tree repaint.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigIconProvider implements IconDescriptorUpdater {
    private static final int FLAG_CONTROLLER = 1;
    private static final int FLAG_EXTENDS = 2;
    private static final int FLAG_INCLUDE = 4;

    /**
     * Pre-built layered icon instances for all 8 badge bitmask combinations.
     * Index 0 = no badges → null. Bits: 0 = controller, 1 = extends, 2 = include.
     *
     * Lazy (holder class) so icon resources are only loaded on first access (after the platform is ready).
     */
    private static final class TwigBadgeIcons {
        private static final @Nullable Image[] ICONS = buildTwigBadgeIcons();
    }

    private static @Nullable Image[] buildTwigBadgeIcons() {
        @Nullable Image[] icons = new Image[8];
        for (int key = 1; key < icons.length; key++) {
            icons[key] = buildTwigBadgeIcon(key);
        }
        return icons;
    }

    private static Image buildTwigBadgeIcon(int key) {
        List<Image> layers = new ArrayList<>();
        layers.add(TwigIcons.TwigFileIcon);

        // TODO consulo-port: ImageEffects.layered has no per-layer position; upstream LayeredIcon placed the badges at
        //  SwingConstants.NORTH_WEST (controller), SOUTH_WEST (extends) and SOUTH_EAST (include)
        if ((key & FLAG_CONTROLLER) != 0) {
            layers.add(Symfony2Icons.TWIG_CONTROLLER_FILE);
        }
        if ((key & FLAG_EXTENDS) != 0) {
            layers.add(Symfony2Icons.TWIG_EXTENDS_FILE);
        }
        if ((key & FLAG_INCLUDE) != 0) {
            layers.add(Symfony2Icons.TWIG_IMPLEMENTS_FILE);
        }

        return ImageEffects.layered(layers.toArray(new Image[0]));
    }

    @RequiredReadAction
    @Override
    public void updateIcon(IconDescriptor iconDescriptor, PsiElement element, int flags) {
        Image icon = getIcon(element, flags);
        if (icon != null) {
            iconDescriptor.setMainIcon(icon);
        }
    }

    public @Nullable Image getIcon(PsiElement element, @Iconable.IconFlags int flags) {
        if (!(element instanceof TwigFile twigFile)) {
            return null;
        }

        Project project = twigFile.getProject();

        Settings settings = Settings.getEnabledInstance(twigFile);
        if (settings == null
            || !settings.featureTwigIcon
            || DumbService.getInstance(project).isDumb()
        ) {
            return null;
        }

        VirtualFile virtualFile = twigFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Set<String> templateNames = new LinkedHashSet<>(TwigUtil.getTemplateNamesForFile(project, virtualFile));
        if (templateNames.isEmpty()) {
            return null;
        }

        int key = (hasController(twigFile, templateNames) ? FLAG_CONTROLLER : 0)
            | (hasFileExtendsTag(project, virtualFile) ? FLAG_EXTENDS : 0)
            | (isIncludedByOtherTemplates(project, templateNames) ? FLAG_INCLUDE : 0);

        return TwigBadgeIcons.ICONS[key];
    }

    private boolean hasFileExtendsTag(Project project, VirtualFile virtualFile) {
        return !FileBasedIndex.getInstance().getFileData(TwigExtendsStubIndex.KEY, virtualFile, project).isEmpty();
    }

    private boolean hasController(TwigFile twigFile, Collection<String> templateNames) {
        return !TwigUtil.getTwigFileMethodUsageOnIndex(twigFile.getProject(), templateNames).isEmpty()
            || !TwigUtil.findTwigFileController(twigFile).isEmpty();
    }

    private boolean isIncludedByOtherTemplates(Project project, Collection<String> templateNames) {
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.allScope(project),
            TwigFileType.INSTANCE
        );

        for (String name : templateNames) {
            if (!FileBasedIndex.getInstance().getContainingFiles(TwigIncludeStubIndex.KEY, name, scope).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
