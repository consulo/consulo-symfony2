package fr.adrienbrault.idea.symfony2plugin.form.action.generator;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony.FormBuilderFieldGeneratorAction",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class FormBuilderFieldGeneratorAction extends CodeInsightAction {
    public FormBuilderFieldGeneratorAction() {
        super(LocalizeValue.localizeTODO("Form Fields"), LocalizeValue.empty(), Symfony2Icons.FORM_TYPE);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        if (!(file instanceof PhpFile) || !Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        Method method = PsiTreeUtil.findElementOfClassAtOffset(
            file,
            editor.getCaretModel().getOffset(),
            Method.class,
            false
        );

        return method != null &&
            "buildForm".equals(method.getName()) &&
            PhpElementsUtil.isMethodInstanceOf(
                method,
                "\\Symfony\\Component\\Form\\FormTypeInterface",
                "buildForm"
            );
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            Method method = PsiTreeUtil.findElementOfClassAtOffset(
                psiFile,
                editor.getCaretModel().getOffset(),
                Method.class,
                false
            );

            if (method == null) {
                return;
            }

            PhpClass phpClass = FormOptionsUtil.getFormPhpClassFromContext(method);
            if (phpClass == null) {
                IdeHelper.showErrorHintIfAvailable(editor, "No data_class option context found");
                return;
            }

            List<JBFormFieldItem> jbFormFieldItems = new ArrayList<>();
            FormUnderscoreMethodReference.visitPropertyPath(
                phpClass,
                pair -> jbFormFieldItems.add(new JBFormFieldItem(pair.getFirst(), pair.getSecond()))
            );

            JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(jbFormFieldItems)
                .setTitle("Symfony: Select Fields")
                .setItemsChosenCallback(strings ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, () ->
                        insertSelectedNamedElements(project, method, editor, strings)
                    )
                )
                .createPopup();

            editor.showPopupInBestPositionFor(popup);
        }

        private static final class JBFormFieldItem {
            private final String myKey;
            private final PhpNamedElement myPhpNamedElement;

            JBFormFieldItem(String key, PhpNamedElement phpNamedElement) {
                myKey = key;
                myPhpNamedElement = phpNamedElement;
            }

            public String getKey() {
                return myKey;
            }

            public PhpNamedElement getPhpNamedElement() {
                return myPhpNamedElement;
            }

            @Override
            public boolean equals(@Nullable Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof JBFormFieldItem that)) {
                    return false;
                }
                return myKey.equals(that.myKey) && myPhpNamedElement.equals(that.myPhpNamedElement);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myKey, myPhpNamedElement);
            }

            @Override
            public String toString() {
                return myKey;
            }
        }

        private void insertSelectedNamedElements(
            Project project,
            Method method,
            Editor editor,
            Collection<JBFormFieldItem> strings
        ) {
            String formBuilderVariable = findFormBuilderVariable(project, method);
            if (formBuilderVariable == null) {
                return;
            }

            PhpIndex instance = PhpIndex.getInstance(project);

            StringBuilder content = new StringBuilder();
            for (JBFormFieldItem field : strings) {
                Pair<String, Map<String, String>> guessed = FormUtil.getGuessedFormFieldParameters(
                    instance,
                    project,
                    field.getKey(),
                    field.getPhpNamedElement()
                );

                String guessedTypeClass = guessed.getFirst();
                Map<String, String> options = guessed.getSecond();

                content.append("$").append(formBuilderVariable).append("->add('").append(field.getKey()).append("'");

                if (guessedTypeClass != null) {
                    content.append(", ");
                    content.append(PhpElementsUtil.insertUseIfNecessary(method, guessedTypeClass));
                    content.append("::class");
                }

                if (!options.isEmpty()) {
                    content.append(", [");
                    content.append(options.entrySet().stream().map(entry -> {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if ("class".equals(key)) {
                            String classUse = PhpElementsUtil.insertUseIfNecessary(method, value);
                            return "'" + key + "' => " + classUse + "::class";
                        }
                        else {
                            return "'" + key + "' => " + value;
                        }
                    }).collect(Collectors.joining(", ")));
                    content.append("]");
                }

                content.append(");\n");
            }

            int caretModel = editor.getCaretModel().getOffset();

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

            PhpInsertHandlerUtil.insertStringAtCaret(editor, content.toString());

            CodeStyleManager.getInstance(project)
                .reformatText(method.getContainingFile(), caretModel, caretModel + content.length());
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        }

        private @Nullable String findFormBuilderVariable(Project project, Method method) {
            for (Parameter parameter : method.getParameters()) {
                if (parameter.getType().isConvertibleFrom(
                    project,
                    new PhpType().add("\\Symfony\\Component\\Form\\FormBuilderInterface")
                )) {
                    return parameter.getName();
                }
            }

            return null;
        }
    }
}
