package fr.adrienbrault.idea.symfony2plugin.templating.action;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import com.jetbrains.twig.icons.TwigIcons;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyTwigExtendsGenerator", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_GENERATE)))
public final class TwigExtendsGenerator extends CodeInsightAction {
    public TwigExtendsGenerator() {
        super(LocalizeValue.localizeTODO("Twig: Extends"), LocalizeValue.empty(), TwigIcons.TwigFileIcon);
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return Symfony2ProjectComponent.isEnabled(project) &&
            (file instanceof TwigFile ||
                file instanceof HtmlFileImpl && StringUtil.endsWithIgnoreCase(file.getName(), ".twig") ||
                TwigUtil.getInjectedTwigElement(file, editor) != null);
    }

    private static final class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile file) {
            if (TwigUtil.getInjectedTwigElement(file, editor) == null) {
                return;
            }
            List<String> prioritizedKeys = TwigUtil.getExtendsTemplateUsageAsOrderedList(project);

            if (prioritizedKeys.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "No extends found");
                return;
            }

            editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(prioritizedKeys)
                .setTitle("Symfony: Twig Extends")
                .setItemsChosenCallback((Set<String> strings) -> {
                    try {
                        WriteCommandAction.writeCommandAction(editor.getProject())
                            .withName("Twig Extends")
                            .run(() -> {
                                String content = strings.stream()
                                    .map(string -> "{% extends '" + string + "' %}")
                                    .collect(Collectors.joining("\n"));

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content);
                            });
                    }
                    catch (Throwable ignored) {
                    }
                })
                .createPopup());
        }
    }
}
