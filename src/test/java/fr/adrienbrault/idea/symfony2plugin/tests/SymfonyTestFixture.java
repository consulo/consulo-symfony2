/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.adrienbrault.idea.symfony2plugin.tests;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.application.WriteAction;
import consulo.it.CodeInsightTestFixture;
import consulo.it.daemon.ExpectedHighlightingData;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.it.HeadlessProjects;
import consulo.it.HeadlessUIThread;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import java.util.Map;
import java.util.IdentityHashMap;
import org.opentest4j.AssertionFailedError;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The {@code myFixture} of the plugin's tests, in the shape of IntelliJ's code insight fixture: files are written
 * under the project root, which is the content root of the module, and {@code <caret>} marks the caret of a configured file.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
public final class SymfonyTestFixture {
    public static final String CARET = "<caret>";

    private final CodeInsightTestFixture myFixture;
    private final Supplier<String> myTestDataPath;

    private static final Pattern HIGHLIGHTING_MARKUP = Pattern.compile("<(error|warning|weak_warning|info|inject|symbolName|lineMarker)[\\s>]");

    private @Nullable PsiFile myFile;
    private @Nullable String myConfiguredName;
    private @Nullable String myConfiguredText;
    private int myCaretOffset;
    private @Nullable Editor myEditor;
    private @Nullable List<LookupElement> myLookupElements;
    // the prefix each lookup element was matched with, which its insertion replaces
    private final Map<LookupElement, String> myLookupPrefixes = new IdentityHashMap<>();

    SymfonyTestFixture(CodeInsightTestFixture fixture, Supplier<String> testDataPath) {
        myFixture = fixture;
        myTestDataPath = testDataPath;
    }

    public Project getProject() {
        return myFixture.getProject();
    }

    public Module getModule() {
        return myFixture.getModule();
    }

    public String getTestDataPath() {
        return myTestDataPath.get();
    }

    public PsiFile addFileToProject(String relativePath, String text) {
        try {
            return TestReadLock.withoutRead(() -> {
                // a file written before - and maybe opened, like by the editor of a completion - is replaced by a new one:
                // its loaded document would keep the old text
                String path = relativePath;
                VirtualFile existing = myFixture.getProjectRoot().findFileByRelativePath(path);
                if (existing != null) {
                    WriteAction.run(() -> existing.delete(this));
                }
                return myFixture.addFileToProject(path, text);
            });
        }
        catch (Exception e) {
            throw new AssertionError("cannot write " + relativePath, e);
        }
    }

    /**
     * A file in the project directory
     */
    public PsiFile addFileToProjectRoot(String relativePath, String text) {
        try {
            return TestReadLock.withoutRead(() -> myFixture.addFileToProject(relativePath, text));
        }
        catch (Exception e) {
            throw new AssertionError("cannot write " + relativePath, e);
        }
    }

    public VirtualFile copyFileToProject(String sourcePath) {
        return copyFileToProject(sourcePath, sourcePath);
    }

    public VirtualFile copyFileToProject(String sourcePath, String targetPath) {
        Path source = Path.of(getTestDataPath(), sourcePath);
        String text;
        try {
            text = Files.readString(source, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            throw new AssertionError("cannot read the test data " + source, e);
        }
        return addFileToProject(targetPath, text).getVirtualFile();
    }

    public PsiFile configureByText(FileType fileType, String text) {
        return configureByText("aaa." + fileType.getDefaultExtension(), text);
    }

    public PsiFile configureByText(LanguageFileType fileType, String text) {
        return configureByText((FileType) fileType, text);
    }

    public PsiFile configureByText(String fileName, String text) {
        myConfiguredName = fileName;
        myConfiguredText = text;
        if (HIGHLIGHTING_MARKUP.matcher(text).find()) {
            // expected highlights, checked by checkHighlighting()
            ExpectedHighlightingData expected = ExpectedHighlightingData.parse(text);
            PsiFile file = addFileToProject(fileName, expected.getText());
            configure(file, Math.max(expected.getCaretOffset(), 0));
            return file;
        }
        CaretText caretText = CaretText.parse(text);
        PsiFile file = addFileToProject(fileName, caretText.text());
        configure(file, Math.max(caretText.caret(), 0));
        return file;
    }

    /**
     * The highlights of the file configured last
     */
    public List<HighlightInfo> doHighlighting() {
        String name = myConfiguredName;
        String text = myConfiguredText;
        if (name == null || text == null) {
            throw new IllegalStateException("nothing was configured - call configureByText first");
        }
        try {
            return TestReadLock.withoutRead(() -> {
                myFixture.configureByText(name, text);
                return myFixture.doHighlighting();
            });
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Checks the highlights of the file configured last against its markup - {@code <error descr="...">...</error>}
     */
    public void checkHighlighting() {
        String name = myConfiguredName;
        String text = myConfiguredText;
        if (name == null || text == null) {
            throw new IllegalStateException("nothing was configured - call configureByText first");
        }
        try {
            TestReadLock.withoutRead(() -> {
                myFixture.configureByText(name, text);
                myFixture.checkHighlighting();
                return null;
            });
        }
        catch (Exception e) {
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new AssertionError(e);
        }
    }

    public PsiFile configureByFile(String sourcePath) {
        return configureFromExistingVirtualFile(copyFileToProject(sourcePath));
    }

    /**
     * Makes the file the configured one, its {@code <caret>} marker is taken out
     */
    public PsiFile configureFromExistingVirtualFile(VirtualFile file) {
        PsiFile psiFile = psiFile(file);
        String text = psiFile.getText();
        CaretText caretText = CaretText.parse(text);
        if (caretText.caret() >= 0) {
            psiFile = addFileToProject(relativePath(file), caretText.text());
        }
        configure(psiFile, Math.max(caretText.caret(), 0));
        return psiFile;
    }

    public @Nullable VirtualFile findFileInTempDir(String relativePath) {
        return myFixture.getProjectRoot().findFileByRelativePath(relativePath);
    }

    public PsiFile getFile() {
        if (myFile == null) {
            throw new IllegalStateException("nothing was configured - call configureByText first");
        }
        return myFile;
    }

    public int getCaretOffset() {
        return myCaretOffset;
    }

    /**
     * Moves the caret of the configured file, which completion and navigation work at
     */
    public void moveCaret(int offset) {
        myCaretOffset = offset;
    }

    public Document getDocument() {
        Document document = FileDocumentManager.getInstance().getDocument(getFile().getVirtualFile());
        if (document == null) {
            throw new AssertionError(getFile() + " has no document");
        }
        return document;
    }

    /**
     * An editor on the configured file
     */
    public Editor getEditor() {
        if (myEditor == null) {
            Document document = getDocument();
            myEditor = HeadlessUIThread.compute(() -> EditorFactory.getInstance().createEditor(document, getProject()));
        }
        return myEditor;
    }

    public <T extends PsiElement> @Nullable T findElementByText(String text, Class<T> elementClass) {
        int offset = getFile().getText().indexOf(text);
        if (offset < 0) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(getFile().findElementAt(offset), elementClass, false);
    }

    /**
     * The basic completion at the caret, computed like the IDE does: on a copy of the file with the dummy identifier at
     * the caret
     */
    public LookupElement[] completeBasic() {
        PsiFile file = getFile();
        int offset = myCaretOffset;
        String original = file.getText();
        String copyText = original.substring(0, offset) + CompletionInitializationContext.DUMMY_IDENTIFIER + original.substring(offset);
        PsiFile copy = PsiFileFactory.getInstance(getProject())
            .createFileFromText(file.getName(), file.getFileType(), copyText, LocalTimeCounter.currentTime(), false, true);
        ((PsiFileImpl) copy).setOriginalFile(file);
        PsiElement position = copy.findElementAt(offset);
        if (position == null) {
            throw new AssertionError("no element at the caret of the completion copy of " + file.getName());
        }

        CompletionParameters parameters = new CompletionParameters(position, file, CompletionType.BASIC, offset, 1, getEditor(), () -> false);
        List<LookupElement> lookupElements = new ArrayList<>();
        myLookupPrefixes.clear();
        CompletionService.getCompletionService().performCompletion(parameters, result -> {
            lookupElements.add(result.getLookupElement());
            myLookupPrefixes.put(result.getLookupElement(), result.getPrefixMatcher().getPrefix());
        });
        myLookupElements = lookupElements;
        return lookupElements.toArray(LookupElement.EMPTY_ARRAY);
    }

    /**
     * The basic completion, a single variant inserted right away like the IDE does
     */
    public LookupElement[] completeBasicAndInsertSingle() {
        LookupElement[] lookupElements = completeBasic();
        if (lookupElements.length == 1) {
            insertLookupElement(lookupElements[0]);
        }
        return lookupElements;
    }

    /**
     * Inserts a lookup element at the caret like the IDE: its lookup string replaces the prefix it was matched with, then its
     * insert handler runs; the caret ends where the handler leaves it
     */
    public void insertLookupElement(LookupElement element) {
        String prefix = myLookupPrefixes.getOrDefault(element, "");
        Document document = getDocument();
        Editor editor = getEditor();
        int end = myCaretOffset;
        int start = end - prefix.length();

        runWriteCommand(() -> {
            document.replaceString(start, end, element.getLookupString());
            int identifierEnd = start + element.getLookupString().length();
            editor.getCaretModel().moveToOffset(identifierEnd);
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);

            OffsetMap offsetMap = new OffsetMap(document);
            offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, start);
            offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, identifierEnd);
            offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, identifierEnd);
            InsertionContext context = new InsertionContext(
                offsetMap, Lookup.NORMAL_SELECT_CHAR, new LookupElement[]{element}, getFile(), editor, false
            );
            element.handleInsert(context);
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
        myCaretOffset = editor.getCaretModel().getOffset();
    }

    /**
     * The text of the configured file is the expected one; a {@code <caret>} marker in it is checked against the caret
     */
    public void checkResult(String expected) {
        CaretText expectedCaretText = CaretText.parse(expected);
        int expectedCaret = expectedCaretText.caret();
        String expectedText = expectedCaretText.text();
        String actualText = getDocument().getText();
        if (!expectedText.equals(actualText)) {
            throw new AssertionFailedError(
                "the text of " + getFile().getName() + " differs ==> expected: <" + expectedText + "> but was: <" + actualText + ">"
                    + " (lookup elements: " + getLookupElementStrings() + ")",
                expectedText,
                actualText
            );
        }
        if (expectedCaret >= 0 && expectedCaret != myCaretOffset) {
            throw new AssertionFailedError("the caret of " + getFile().getName() + " differs", expectedCaret, myCaretOffset);
        }
    }

    /**
     * A write command on the UI thread, without the read lock of the test body
     */
    private void runWriteCommand(Runnable runnable) {
        try {
            TestReadLock.withoutRead(() -> {
                HeadlessUIThread.run(() -> WriteCommandAction.runWriteCommandAction(getProject(), runnable));
                return null;
            });
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public LookupElement @Nullable [] getLookupElements() {
        return myLookupElements == null ? null : myLookupElements.toArray(LookupElement.EMPTY_ARRAY);
    }

    public @Nullable List<String> getLookupElementStrings() {
        if (myLookupElements == null) {
            return null;
        }
        List<String> strings = new ArrayList<>();
        for (LookupElement element : myLookupElements) {
            strings.add(element.getLookupString());
        }
        return strings;
    }

    void tearDown() {
        releaseEditor();
    }

    private void configure(PsiFile file, int caretOffset) {
        releaseEditor();
        myFile = file;
        myCaretOffset = caretOffset;
        myLookupElements = null;
    }

    private void releaseEditor() {
        Editor editor = myEditor;
        if (editor != null) {
            myEditor = null;
            HeadlessUIThread.run(() -> EditorFactory.getInstance().releaseEditor(editor));
        }
    }

    private PsiFile psiFile(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
        if (psiFile == null) {
            throw new AssertionError(file + " has no psi");
        }
        return psiFile;
    }

    private String relativePath(VirtualFile file) {
        String root = myFixture.getProjectRoot().getPath();
        return file.getPath().substring(root.length() + 1);
    }

    /**
     * A text without its {@code <caret>} markers; with several markers the caret is the last one, the primary caret of upstream's
     * fixture
     *
     * @param caret the offset of the caret in the text, {@code -1} without a marker
     */
    record CaretText(String text, int caret) {
        static CaretText parse(String marked) {
            StringBuilder text = new StringBuilder(marked.length());
            int caret = -1;
            int from = 0;
            int marker;
            while ((marker = marked.indexOf(CARET, from)) >= 0) {
                text.append(marked, from, marker);
                caret = text.length();
                from = marker + CARET.length();
            }
            text.append(marked, from, marked.length());
            return new CaretText(text.toString(), caret);
        }
    }
}
