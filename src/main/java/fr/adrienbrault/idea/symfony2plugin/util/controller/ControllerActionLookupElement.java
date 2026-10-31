package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.jetbrains.php.PhpIcons;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerActionLookupElement extends LookupElement {

    private final ControllerAction controllerAction;

    public ControllerActionLookupElement(ControllerAction controllerAction) {
        this.controllerAction = controllerAction;
    }

    @Override
    public String getLookupString() {
        return this.controllerAction.getShortcutName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(StringUtils.stripStart(controllerAction.getMethod().getFQN(), "\\"));
        presentation.setTypeGrayed(true);
        presentation.setIcon(PhpIcons.METHOD);
    }

}