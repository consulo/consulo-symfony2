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
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "SymfonyTwigBlockOverwrite", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_GENERATE)))
public final class TwigBlockOverwriteGenerator extends CodeInsightAction {
    public TwigBlockOverwriteGenerator() {
        super(LocalizeValue.localizeTODO("Twig: Block Overwrite"), LocalizeValue.empty(), Symfony2Icons.TWIG_BLOCK_OVERWRITE);
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
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(file, editor);
            if (psiElement == null) {
                return;
            }

            // collect blocks in all related files
            Pair<Collection<PsiFile>, Boolean> scopedContext = TwigUtil.findScopedFile(psiElement);

            Collection<LookupElement> blockLookupElements = TwigUtil.getBlockLookupElements(
                project,
                TwigFileUtil.collectParentFiles(scopedContext.getSecond(), scopedContext.getFirst())
            );

            List<String> items = blockLookupElements.stream()
                .map(LookupElement::getLookupString)
                .distinct()
                .collect(Collectors.toList());

            if (items.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "No block found");
                return;
            }

            editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(items)
                .setTitle("Symfony: Twig Blocks")
                .setItemsChosenCallback((Set<String> strings) -> {
                    try {
                        String titleBlocks = StringUtils.abbreviate(String.join(", ", strings), 10);

                        WriteCommandAction.writeCommandAction(editor.getProject())
                            .withName("Block Overwrite: " + titleBlocks)
                            .run(() -> {
                                String endBlock = "{% endblock %}";

                                String content = strings.stream()
                                    .map(string -> "{% block " + string + " %}" + endBlock)
                                    .collect(Collectors.joining("\n"));

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content);

                                // move caret inside block
                                // {% block %}<caret>{% endblock %}
                                editor.getCaretModel().moveCaretRelatively(-endBlock.length(), 0, false, false, true);
                            });
                    }
                    catch (Throwable ignored) {
                    }
                })
                .createPopup());
        }
    }
}
