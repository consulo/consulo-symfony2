package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import com.jetbrains.twig.icons.TwigIcons;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Attach "Foo\FoobarController::FooAction" to its controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after TemplatesControllerRelatedGotoCollector")
public class TwigControllerUsageControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {
    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        Method method = parameter.getMethod();
        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return;
        }

        String controllerAction = StringUtils.stripStart(containingClass.getPresentableFQN(), "\\") + "::" + method.getName();

        Set<VirtualFile> targets = new HashSet<>();
        FileBasedIndex.getInstance().getFilesWithKey(
            TwigControllerStubIndex.KEY,
            new HashSet<>(Set.of(controllerAction)),
            virtualFile -> {
                targets.add(virtualFile);
                return true;
            },
            GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.allScope(parameter.getProject()),
                TwigFileType.INSTANCE
            )
        );

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(parameter.getProject(), targets)) {
            TwigUtil.visitControllerFunctions(psiFile, pair -> {
                if (pair.getFirst().equalsIgnoreCase(controllerAction)) {
                    parameter.add(
                        new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(pair.getSecond())
                            .withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER)
                    );
                }
            });
        }
    }
}
