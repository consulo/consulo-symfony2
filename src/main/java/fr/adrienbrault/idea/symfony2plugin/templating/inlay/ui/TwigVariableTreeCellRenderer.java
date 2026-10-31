package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.jetbrains.php.PhpIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableTreeCellRenderer {
    private TwigVariableTreeCellRenderer() {
    }

    public static void render(TwigVariableTreeNode node, TextItemPresentation presentation) {
        Image icon;
        if (node.isList()) {
            icon = PlatformIconGroup.uidesignerTable(); // TODO consulo-port: no AllIcons.Nodes.DataTables icon
        }
        else if (node.isBool()) {
            icon = PlatformIconGroup.nodesVariable();
        }
        else if (node.isPropertyNode()) {
            icon = PhpIcons.CLASS_ATTRIBUTE;
        }
        else {
            icon = PhpIcons.CLASS;
        }
        presentation.withIcon(icon);

        presentation.append(node.name(), TextAttribute.REGULAR);
        presentation.append("  " + node.getTypeDisplay(), TextAttribute.GRAYED);
    }
}
