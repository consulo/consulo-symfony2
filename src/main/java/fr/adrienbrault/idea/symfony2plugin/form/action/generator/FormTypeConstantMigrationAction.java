package fr.adrienbrault.idea.symfony2plugin.form.action.generator;

import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony2.FormTypeMigrationGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class FormTypeConstantMigrationAction extends CodeInsightAction {
    public FormTypeConstantMigrationAction() {
        super(LocalizeValue.localizeTODO("Class constants migration"), LocalizeValue.empty(), Symfony2Icons.FORM_TYPE);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        if (!(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        PhpClass phpClass = PhpCodeEditUtil.findClassAtCaret(editor, file);
        return phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, "Symfony\\Component\\Form\\FormTypeInterface");
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            PhpClass phpClass = PhpCodeEditUtil.findClassAtCaret(editor, psiFile);
            if (phpClass == null) {
                IdeHelper.showErrorHintIfAvailable(editor, "No class context found");
                return;
            }

            List<StringLiteralExpression> formTypes = new ArrayList<>();
            phpClass.acceptChildren(new FormTypeStringElementVisitor(formTypes));

            if (formTypes.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "Nothing to do for me");
                return;
            }

            for (StringLiteralExpression formType : formTypes) {
                try {
                    FormUtil.replaceFormStringAliasWithClassConstant(formType);
                }
                catch (Exception ignored) {
                }
            }
        }

        private static class FormTypeStringElementVisitor extends PsiRecursiveElementVisitor {
            private final Collection<StringLiteralExpression> myFormTypes;

            FormTypeStringElementVisitor(Collection<StringLiteralExpression> formTypes) {
                myFormTypes = formTypes;
            }

            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression &&
                    !StringUtils.isBlank(stringLiteralExpression.getContents()) &&
                    new MethodMatcher.StringParameterMatcher(element, 1)
                        .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
                        .match() != null
                ) {
                    myFormTypes.add(stringLiteralExpression);
                }

                super.visitElement(element);
            }
        }
    }
}
