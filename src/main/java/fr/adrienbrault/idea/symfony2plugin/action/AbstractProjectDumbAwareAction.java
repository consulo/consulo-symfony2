package fr.adrienbrault.idea.symfony2plugin.action;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractProjectDumbAwareAction extends DumbAwareAction implements AnActionWithAsyncUpdate {

    public AbstractProjectDumbAwareAction(String text, String description, Image phpFile) {
        super(LocalizeValue.localizeTODO(text), LocalizeValue.localizeTODO(description), phpFile);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(ActionSafeReadLock.run(e, presentation -> update(e)));
    }

    public void update(AnActionEvent event) {
        setStatus(event, false);

        Project project = event.getData(Project.KEY);
        if (Symfony2ProjectComponent.isEnabled(project)) {
            setStatus(event, true);
        }
    }

    protected void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }
}
