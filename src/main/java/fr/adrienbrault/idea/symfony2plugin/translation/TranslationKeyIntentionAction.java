package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.BaseIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyIntentionAction extends BaseIntentionAction {
    private final PsiFile psiFile;

    private final String keyName;

    /**
     * @param psiFile Translation file as yaml
     * @param keyName key name like "translation" or "translation.sub.name"
     */
    public TranslationKeyIntentionAction(PsiFile psiFile, String keyName) {
        this.psiFile = psiFile;
        this.keyName = keyName;
    }

    @Override
    public LocalizeValue getText() {
        String filename = psiFile.getName();

        // try to find suitable presentable filename
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if(virtualFile != null) {
            filename = virtualFile.getPath();
            String relativePath = VirtualFileUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(psiFile), '/');
            if(relativePath != null) {
                filename =  relativePath;
            }
        }

        return LocalizeValue.of("Create translation key: " + filename);
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
        VirtualFile virtualFile = TranslationKeyIntentionAction.this.psiFile.getVirtualFile();
        if(virtualFile == null) {
            return;
        }

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if(psiFile == null) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(psiFile.getProject())
            .name(LocalizeValue.of("Translation insert " + psiFile.getName()))
            .inWriteAction()
            .run(() -> {
                TranslationInsertUtil.invokeTranslation(psiFile, keyName, keyName);
            });
    }
}
