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
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony2.ServiceGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class ServiceGenerateAction extends CodeInsightAction {
    public ServiceGenerateAction() {
        super(LocalizeValue.localizeTODO("Generate Service"), LocalizeValue.empty(), Symfony2Icons.SYMFONY);
    }

    @Override
    public void update(AnActionEvent event) {
        if (!Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY))) {
            event.getPresentation().setEnabled(false);
            return;
        }

        // let main implementation decide on file scope if action is enabled
        super.update(event);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return isValidForPhpClass(editor, file) || isValidForFile(file);
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            @RequiredUIAccess
            public void invoke(Project project, Editor editor, PsiFile psiFile) {
                if (invokePhpClass(project, editor)) {
                    return;
                }

                if (isValidForFile(psiFile)) {
                    invokeFile(project, editor);
                }
            }

            @Override
            public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
                return null;
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    private boolean isValidForPhpClass(Editor editor, PsiFile file) {
        if (!(file instanceof PhpFile)) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        if (offset <= 0) {
            return false;
        }

        PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null) {
            return false;
        }

        return PlatformPatterns.psiElement().inside(PhpClass.class).accepts(psiElement);
    }

    private boolean isValidForFile(PsiFile file) {
        if (file instanceof XmlFile xmlFile) {
            XmlTag rootTag = xmlFile.getRootTag();
            return rootTag != null && "container".equals(rootTag.getName());
        }
        else if (file instanceof YAMLFile yamlFile) {
            return YAMLUtil.getQualifiedKeyInFile(yamlFile, "parameters") != null ||
                YAMLUtil.getQualifiedKeyInFile(yamlFile, "services") != null;
        }
        else {
            return false;
        }
    }

    @RequiredUIAccess
    private boolean invokeFile(Project project, Editor editor) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return false;
        }

        SymfonyCreateService.create(project, file, editor);

        return true;
    }

    @RequiredUIAccess
    private boolean invokePhpClass(Project project, Editor editor) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        if (offset <= 0) {
            return false;
        }

        PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null) {
            return false;
        }

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return false;
        }

        ServiceGenerateActionKt.invokeServiceGenerator(project, file, phpClass, editor);

        return true;
    }
}
