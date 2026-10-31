package fr.adrienbrault.idea.symfony2plugin.action;

import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.xml.language.psi.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.form.TranslatorKeyExtractorDialog;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(id = "Symfony.TwigLanugage", parents = {
    @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)),
    @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP), anchor = ActionRefAnchor.LAST)
})
public class TwigExtractLanguageAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public TwigExtractLanguageAction() {
        super(
            LocalizeValue.localizeTODO("Extract Translation"),
            LocalizeValue.localizeTODO("Extract Translation Key"),
            Symfony2Icons.SYMFONY
        );
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(ActionSafeReadLock.run(e, presentation -> update(e)));
    }

    public void update(AnActionEvent event) {
        setStatus(event, false);

        if (!Symfony2ProjectComponent.isEnabled(event.getData(Project.KEY))) {
            return;
        }

        PsiFile psiFile = event.getData(PsiFile.KEY);
        if (!(psiFile instanceof TwigFile)) {
            return;
        }

        // consulo-port: update runs in background, read caret and selection from the editor snapshot
        Editor editor = event.getData(EditorKeys.EDITOR_SNAPSHOT);
        if (editor == null) {
            return;
        }

        // find valid PsiElement context, because only html text is a valid extractor action
        PsiElement psiElement;
        if (editor.getSelectionModel().hasSelection()) {
            psiElement = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
        }
        else {
            psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        }

        if (psiElement == null) {
            return;
        }

        // <a title="TEXT">TEXT</a>
        IElementType elementType = psiElement.getNode().getElementType();
        setStatus(
            event,
            elementType == XmlTokenType.XML_DATA_CHARACTERS ||
                elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
        );
    }

    private void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        Editor editor = event.getData(Editor.KEY);
        if (editor == null) {
            return;
        }

        PsiFile psiFile = event.getData(PsiFile.KEY);
        if (!(psiFile instanceof TwigFile twigFile)) {
            return;
        }

        Project project = twigFile.getProject();
        int caretOffset = editor.getCaretModel().getOffset();

        TranslationSelection selection = findTranslationSelection(twigFile, editor);
        if (selection == null) {
            return;
        }

        int startOffset = selection.startOffset();
        int endOffset = selection.endOffset();
        String translationText = selection.text();

        SortedSet<String> domainNames = TranslationUtil.getTranslationDomainLookupElements(project)
            .stream()
            .map(lookupElement -> lookupElement.getLookupString())
            .collect(Collectors.toCollection(TreeSet::new));

        // get default domain on twig tag
        // also pipe it to insert handler; to append it as parameter

        // scope to search translation domain
        PsiElement transDefaultScope = twigFile.findElementAt(caretOffset);
        if (transDefaultScope == null) {
            transDefaultScope = twigFile;
        }

        PsiElement element = TwigUtil.getElementOnTwigViewProvider(transDefaultScope);
        TwigUtil.DomainScope twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : transDefaultScope);

        // we want to have mostly used domain preselected
        String defaultDomain = twigFileDomainScope.getDefaultDomain();
        String reselectedDomain = twigFileDomainScope.getDomain();

        String defaultKey = translationText.length() < 15
            ? translationText.toLowerCase(Locale.ROOT).replace(" ", ".")
            : null;

        TranslatorKeyExtractorDialog.show(
            project,
            twigFile,
            domainNames,
            defaultKey,
            reselectedDomain,
            new MyOnOkCallback(project, editor, defaultDomain, startOffset, endOffset, translationText)
        );
    }

    private @Nullable TranslationSelection findTranslationSelection(TwigFile psiFile, Editor editor) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText != null) {
            return new TranslationSelection(
                editor.getSelectionModel().getSelectionStart(),
                editor.getSelectionModel().getSelectionEnd(),
                selectedText
            );
        }

        // use dont selected text, so find common PsiElement
        PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (psiElement == null) {
            return null;
        }

        IElementType elementType = psiElement.getNode().getElementType();
        if (elementType != XmlTokenType.XML_DATA_CHARACTERS && elementType != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return null;
        }

        return new TranslationSelection(psiElement.getTextRange().getStartOffset(), psiElement.getTextRange().getEndOffset(), psiElement.getText());
    }

    private record TranslationSelection(int startOffset, int endOffset, String text) {
    }

    private record MyOnOkCallback(
        Project project,
        Editor editor,
        String finalDefaultDomain,
        int finalStartOffset,
        int finalEndOffset,
        String finalTranslationText
    ) implements TranslatorKeyExtractorDialog.OnOkCallback {
        @Override
        public void onClick(
            List<TranslationFileModel> files,
            String keyName,
            String domain,
            boolean navigateTo
        ) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

            // insert Twig trans key
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(LocalizeValue.of("Twig Translation Insert " + keyName))
                .inWriteAction()
                .run(() -> {
                    String insertString;
                    if (finalDefaultDomain.equals(domain)) {
                        insertString = "{{ '" + keyName + "'|trans }}";
                    }
                    else {
                        insertString = "{{ '" + keyName + "'|trans({}, '" + domain + "') }}";
                    }

                    editor.getDocument().replaceString(finalStartOffset, finalEndOffset, insertString);
                    editor.getCaretModel().moveToOffset(finalEndOffset);
                });

            List<PsiElement> targets = new ArrayList<>();

            // so finally insert it; first file can be a navigation target
            for (TranslationFileModel transPsiFile : files) {
                PsiFile file = transPsiFile.getPsiFile();

                CommandProcessor.getInstance().newCommand()
                    .project(file.getProject())
                    .name(LocalizeValue.of("Translation Insert " + file.getName()))
                    .inWriteAction()
                    .run(() -> {
                        PsiElement target = TranslationInsertUtil.invokeTranslation(file, keyName, finalTranslationText);
                        if (target != null) {
                            targets.add(target);
                        }
                    });
            }

            if (navigateTo && !targets.isEmpty()) {
                PsiDocumentManager.getInstance(project).commitAndRunReadAction(() -> IdeHelper.navigateToPsiElement(targets.get(0)));
            }
        }
    }
}
