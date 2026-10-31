package fr.adrienbrault.idea.symfony2plugin.twig.action;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony.TwigFormFieldGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class TwigFormFieldGenerator extends CodeInsightAction {
    public TwigFormFieldGenerator() {
        super(LocalizeValue.localizeTODO("Form Fields"), LocalizeValue.empty(), Symfony2Icons.FORM_TYPE);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        boolean isTwigContext = Symfony2ProjectComponent.isEnabled(project) && (
            file instanceof TwigFile ||
                file instanceof HtmlFileImpl && StringUtil.endsWithIgnoreCase(file.getName(), ".twig") ||
                TwigUtil.getInjectedTwigElement(file, editor) != null
        );

        if (!isTwigContext) {
            return false;
        }

        PsiElement psiElement = TwigUtil.getInjectedTwigElement(file, editor);
        if (psiElement == null) {
            return false;
        }

        return TwigTypeResolveUtil.collectScopeVariables(psiElement).values().stream().anyMatch(it -> isFormView(it, project));
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor);
            if (psiElement == null) {
                return;
            }

            List<JBFormFieldItem> phpClasses = new ArrayList<>();
            for (Map.Entry<String, PsiVariable> entry : TwigTypeResolveUtil.collectScopeVariables(psiElement).entrySet()) {
                String key = entry.getKey();
                PsiVariable variable = entry.getValue();
                if (!isFormView(variable, project)) {
                    continue;
                }

                for (String formTypeFqn : variable.getFormTypeFqns()) {
                    PhpClass phpClass = PhpElementsUtil.getClassInterface(project, formTypeFqn);
                    if (phpClass != null) {
                        phpClasses.add(new JBFormFieldItem(key, phpClass));
                    }
                }
            }

            if (phpClasses.size() == 1) {
                extracted(project, editor, phpClasses.get(0));
                return;
            }

            JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(phpClasses))
                .setTitle("Symfony: Select FormType")
                .setItemChosenCallback(item ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, () ->
                        extracted(project, editor, item)
                    )
                )
                .createPopup();

            editor.showPopupInBestPositionFor(popup);
        }

        @RequiredUIAccess
        private void extracted(Project project, Editor editor, JBFormFieldItem next) {
            Set<@Nullable String> fields = new HashSet<>();
            FormFieldResolver.visitFormReferencesFields(next.getPhpClass(), twigTypeContainer ->
                fields.add(twigTypeContainer.getStringElement())
            );

            JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(fields))
                .setTitle("Symfony: Select Form Fields \"" + StringUtils.abbreviate(next.getPhpClass().getName(), 20) + "\"")
                .setItemsChosenCallback(strings ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, () -> {
                        String content = strings.stream()
                            .map(field -> "{{ form_row(" + next.getKey() + "." + field + ") }}\n")
                            .collect(Collectors.joining(""));

                        PhpInsertHandlerUtil.insertStringAtCaret(editor, content);
                    })
                )
                .createPopup();

            editor.showPopupInBestPositionFor(popup);
        }
    }

    public static final class JBFormFieldItem {
        private final String key;
        private final PhpClass phpClass;

        public JBFormFieldItem(String key, PhpClass phpClass) {
            this.key = key;
            this.phpClass = phpClass;
        }

        public String getKey() {
            return key;
        }

        public PhpClass getPhpClass() {
            return phpClass;
        }

        public String component1() {
            return key;
        }

        public PhpClass component2() {
            return phpClass;
        }

        public JBFormFieldItem copy(String key, PhpClass phpClass) {
            return new JBFormFieldItem(key, phpClass);
        }

        @Override
        public String toString() {
            return key + " => " + phpClass.getName();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof JBFormFieldItem that)) {
                return false;
            }
            return key.equals(that.key) && phpClass.equals(that.phpClass);
        }

        @Override
        public int hashCode() {
            return key.hashCode() * 31 + phpClass.hashCode();
        }
    }

    private static boolean isFormView(PsiVariable psiVariable, Project project) {
        if (psiVariable.getFormTypeFqns().isEmpty()) {
            return false;
        }

        PhpType phpType = PhpIndex.getInstance(project).completeType(
            project,
            PhpType.from(psiVariable.getTypes().toArray(new String[0])),
            new HashSet<>()
        );
        return FormFieldResolver.isFormView(phpType);
    }
}
