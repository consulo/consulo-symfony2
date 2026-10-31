package fr.adrienbrault.idea.symfony2plugin.intentions.ui;

import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.ui.ex.popup.JBPopupFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceSuggestDialog {

    public static void create(final Editor editor, Collection<String> services, final Callback callback) {
        final List<String> list = new ArrayList<>(services);

        editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(list)
            .setTitle("Symfony: Service Suggestion")
            .setItemChosenCallback(s -> WriteCommandAction.runWriteCommandAction(editor.getProject(), "Service Suggestion Insert", null, () -> callback.insert(s)))
            .createPopup());
    }

    public interface Callback {
        void insert(String selected);
    }
}
