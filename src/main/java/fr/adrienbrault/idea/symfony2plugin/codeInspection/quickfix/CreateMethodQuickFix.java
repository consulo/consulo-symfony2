package fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix;

import com.jetbrains.php.lang.PhpCodeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.FileEditorManager;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CreateMethodQuickFix implements LocalQuickFix {
    private final SmartPsiElementPointer<PhpClass> smartPhpClass;

    private final String functionName;

    private final InsertStringInterface stringInterface;

    public CreateMethodQuickFix(PhpClass phpClass, String functionName, InsertStringInterface stringInterface) {
        this.smartPhpClass = SmartPointerManager.getInstance(phpClass.getProject()).createSmartPsiElementPointer(phpClass);
        this.functionName = functionName;
        this.stringInterface = stringInterface;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Create Method");
    }

    @Override
    public void applyFix(final Project project, ProblemDescriptor problemDescriptor) {
        PhpClass phpClass = smartPhpClass.getElement();
        if(phpClass == null) {
            return;
        }

        final Method methodCreated = PhpCodeUtil.createMethodFromTemplate(
            phpClass, project, this.stringInterface.getStringBuilder(problemDescriptor, phpClass, functionName).toString()
        );

        if(methodCreated == null) {
            return;
        }

        final Editor editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptorFactory.getInstance(project).newBuilder(phpClass.getContainingFile().getVirtualFile()).build(), true);
        if(editor == null) {
            return;
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

        final int insertPos = CodeUtil.getMethodInsertPosition(phpClass, functionName);
        if(insertPos == -1) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, "Create Method", null, () -> {
            StringBuffer textBuf = new StringBuffer();
            textBuf.append("\n");
            textBuf.append(methodCreated.getText());

            editor.getDocument().insertString(insertPos, textBuf);
            final int endPos = insertPos + textBuf.length();

            CodeStyleManager.getInstance(project).reformatText(phpClass.getContainingFile(), insertPos, endPos);
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

            Method insertedMethod = phpClass.findMethodByName(functionName);
            if(insertedMethod != null) {
                editor.getCaretModel().moveToOffset(insertedMethod.getTextRange().getStartOffset());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        });
    }

    public interface InsertStringInterface {
            StringBuilder getStringBuilder(ProblemDescriptor problemDescriptor, PhpClass phpClass, String functionName);
    }
}
