package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionPointName;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ControllerMethodLineMarkerProvider implements LineMarkerProvider {

    private static final ExtensionPointName<ControllerActionGotoRelatedCollector> EP_NAME = ExtensionPointName.create(ControllerActionGotoRelatedCollector.class);

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> results) {
        if (psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for (PsiElement psiElement: psiElements) {
            if (psiElement.getNode().getElementType() != PhpTokenTypes.IDENTIFIER || !(psiElement.getParent() instanceof Method method)) {
                continue;
            }

            LineMarkerInfo<?> lineMarkerInfo = collect(psiElement, method);
            if (lineMarkerInfo != null) {
                results.add(lineMarkerInfo);
            }
        }
    }

    @Nullable
    public LineMarkerInfo<?> collect(PsiElement psiElement, Method method) {
        if (!method.getAccess().isPublic()) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = getGotoRelatedItems(method);
        if (gotoRelatedItems.isEmpty()) {
            return null;
        }

        // only one item don't need popover
        if (gotoRelatedItems.size() == 1) {
            GotoRelatedItem gotoRelatedItem = gotoRelatedItems.get(0);

            // hell: find any possible small icon
            Image icon = null;
            if (gotoRelatedItem instanceof RelatedPopupGotoLineMarker.PopupGotoRelatedItem) {
                icon = ((RelatedPopupGotoLineMarker.PopupGotoRelatedItem) gotoRelatedItem).getSmallIcon();
            }

            if (icon == null) {
               icon = Symfony2Icons.SYMFONY_LINE_MARKER;
            }

            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(icon).
                setTargets(gotoRelatedItems.get(0).getElement());

            String customName = gotoRelatedItems.get(0).getCustomName();
            if (customName != null) {
                builder.setTooltipText(customName);
            }

            return builder.createLineMarkerInfo(psiElement);
        }

        return FileLineMarkerUtil.createLineMarkerInfo(
            psiElement,
            Symfony2Icons.SYMFONY_LINE_MARKER,
            "Related Files",
            new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems),
            () -> "Go to related files"
        );
    }

    public static List<GotoRelatedItem> getGotoRelatedItems(Method method) {
        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        ControllerActionGotoRelatedCollectorParameter parameter = new ControllerActionGotoRelatedCollectorParameter(method, gotoRelatedItems);
        for (ControllerActionGotoRelatedCollector extension : EP_NAME.getExtensions()) {
            extension.collectGotoRelatedItems(parameter);
        }

        return gotoRelatedItems;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
