package fr.adrienbrault.idea.symfony2plugin.action;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.lang.Pair;
import consulo.xml.language.psi.XmlFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "Symfony.Menu", parents = {
    @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)),
    @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP), anchor = ActionRefAnchor.LAST)
})
public class SymfonyContainerServiceBuilder extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public SymfonyContainerServiceBuilder() {
        super(
            LocalizeValue.localizeTODO("Create Service"),
            LocalizeValue.localizeTODO("Generate a new Service definition from class name"),
            Symfony2Icons.SYMFONY
        );
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(ActionSafeReadLock.run(e, presentation -> update(e)));
    }

    public void update(AnActionEvent event) {
        PsiFile psiFile = null;
        if (Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY))) {
            Pair<PsiFile, @Nullable PhpClass> pair = findPhpClass(event);
            if (pair != null) {
                psiFile = pair.getFirst();
            }
        }

        setStatus(event, psiFile instanceof YAMLFile || psiFile instanceof XmlFile || psiFile instanceof PhpFile);
    }

    private void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Editor editor = event.getData(Editor.KEY);

        Pair<PsiFile, @Nullable PhpClass> pair = findPhpClass(event);
        if (pair == null) {
            return;
        }

        PsiFile psiFile = pair.getFirst();
        PhpClass phpClass = pair.getSecond();
        if (phpClass == null) {
            SymfonyCreateService.create(project, psiFile, editor);
            return;
        }

        SymfonyCreateService.create(project, psiFile, phpClass, editor);
    }

    private @Nullable Pair<PsiFile, @Nullable PhpClass> findPhpClass(AnActionEvent event) {
        PsiFile psiFile = event.getData(PsiFile.KEY);
        if (!(psiFile instanceof YAMLFile) && !(psiFile instanceof XmlFile) && !(psiFile instanceof PhpFile)) {
            return null;
        }

        // menu item like ProjectView
        if (ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace())) {
            // fins php class on scope
            PhpClass phpClass = psiFile instanceof PhpFile phpFile ? PhpElementsUtil.getFirstClassFromFile(phpFile) : null;
            return Pair.create(psiFile, phpClass);
        }

        // directly got the class
        PsiElement psiElement = event.getData(PsiElement.KEY);
        if (psiElement instanceof PhpClass elementPhpClass) {
            return Pair.create(psiFile, elementPhpClass);
        }

        // click inside class
        PhpClass phpClass = psiFile instanceof PhpFile phpFile ? PhpElementsUtil.getFirstClassFromFile(phpFile) : null;
        return Pair.create(psiFile, phpClass);
    }
}
