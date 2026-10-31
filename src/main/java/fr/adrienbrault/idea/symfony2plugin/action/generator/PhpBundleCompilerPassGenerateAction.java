package fr.adrienbrault.idea.symfony2plugin.action.generator;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony2.PhpCompilerPassGenerateGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class PhpBundleCompilerPassGenerateAction extends CodeInsightAction {
    public PhpBundleCompilerPassGenerateAction() {
        super(LocalizeValue.localizeTODO("CompilerPass"), LocalizeValue.empty(), Symfony2Icons.SYMFONY);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return file instanceof PhpFile &&
            Symfony2ProjectComponent.isEnabled(project) &&
            PhpBundleFileFactory.getPhpClassForCreateCompilerScope(editor, file) != null;
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            PhpClass phpClass = PhpBundleFileFactory.getPhpClassForCreateCompilerScope(
                editor,
                PsiUtilBase.getPsiFileInEditor(editor, project)
            );

            if (phpClass != null) {
                PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, editor);
            }
        }
    }
}
