package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RelatedPopupGotoLineMarker {

    public static class NavigationHandler implements GutterIconNavigationHandler<PsiElement> {

        private final List<GotoRelatedItem> items;

        public NavigationHandler(List<GotoRelatedItem> items){
            this.items = items;
        }

        @Override
        @RequiredUIAccess
        public void navigate(ComponentEvent<?> e, PsiElement elt) {
            List<GotoRelatedItem>  items = this.items;
            if (items.size() == 1) {
                items.get(0).navigate();
            } else {
                PopupNavigationUtil.getRelatedItemsPopup(items, "Go to Related Files").show(RelativePoint2D.of(e));
            }

        }

    }

    public static class PopupGotoRelatedItem extends GotoRelatedItem {

        private String customName;
        private Image icon;
        private Image smallIcon;

        public PopupGotoRelatedItem(PsiElement element) {
            super(element);
        }

        public PopupGotoRelatedItem(PsiElement element, String customName) {
            super(element);
            this.customName = customName;
        }

        @Nullable
        @Override
        public String getCustomName() {
            return customName;
        }

        @Nullable
        @Override
        public Image getCustomIcon() {
            if(this.icon != null) {
                return this.icon;
            }

            return super.getCustomIcon();
        }

        public PopupGotoRelatedItem withIcon(Image icon, Image smallIcon) {
            this.icon = icon;
            this.smallIcon = smallIcon;
            return this;
        }

        public Image getSmallIcon() {
            return smallIcon;
        }

    }

}
