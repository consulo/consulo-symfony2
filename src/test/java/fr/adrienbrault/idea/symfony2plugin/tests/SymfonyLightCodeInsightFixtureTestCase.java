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

import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpReference;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.document.util.TextRange;
import consulo.it.AllowLogError;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.HeadlessUIThread;
import consulo.language.ast.IElementType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.file.LanguageFileType;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.index.io.ID;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.php.module.extension.PhpMutableModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyMutableModuleExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The base of the plugin's tests, in the shape of upstream's {@code SymfonyLightCodeInsightFixtureTestCase} on
 * IntelliJ's light fixture: a headless project with one module that has the PHP and the Symfony module extensions,
 * {@link #setUp()} and {@link #tearDown()} like JUnit 3, and the body of a test holding the read lock like the
 * event dispatch thread.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtendWith(HeadlessProjectExtension.class)
// TODO the XML plugin outside the IDE: XmlReferenceContributor registers its comment references with a provider type of
//  ide-impl as its last provider
@AllowLogError("consulo.language.impl.internal.psi.resolve.reference.ReferenceProvidersRegistryImpl")
public abstract class SymfonyLightCodeInsightFixtureTestCase extends JUnit3Assert {
    protected SymfonyTestFixture myFixture;

    @BeforeEach
    public final void setUpFixture(CodeInsightTestFixture fixture) throws Exception {
        myFixture = new SymfonyTestFixture(fixture, this::getTestDataPath);
        WriteAction.run(() -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(fixture.getModule()).getModifiableModel();
            PhpMutableModuleExtension<?> php = model.getExtensionWithoutCheck(PhpMutableModuleExtension.class);
            assertNotNull("PHP module extension", php);
            php.setEnabled(true);
            SymfonyMutableModuleExtension symfony = model.getExtensionWithoutCheck(SymfonyMutableModuleExtension.class);
            assertNotNull("Symfony module extension", symfony);
            symfony.setEnabled(true);
            model.commit();
        });
        HeadlessProjects.awaitIdle(fixture.getProject());

        // the setup of a test reads like its body, its writes let the read lock go
        TestReadLock.acquire();
        setUp();
    }

    @AfterEach
    public final void tearDownFixture() throws Exception {
        TestReadLock.release();
        try {
            tearDown();
        }
        finally {
            myFixture.tearDown();
        }
    }

    /**
     * Like JUnit 3: runs before every test, a test class overrides it calling {@code super.setUp()}
     */
    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
    }

    /**
     * Runs code which changes the document itself - like an insert handler - on the UI thread like the IDE, without the
     * read lock of the test body; the test class needs {@link consulo.it.AllowWriteLockUnderUIThread}
     */
    protected void withoutReadLock(Runnable runnable) {
        try {
            TestReadLock.withoutRead(() -> {
                HeadlessUIThread.run(runnable);
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

    /**
     * A write command of the test body on the UI thread like the IDE, which holds the read lock: the lock is let go for
     * the command; the test class needs {@link consulo.it.AllowWriteLockUnderUIThread}
     */
    protected void runWriteCommand(Project project, Runnable runnable) {
        try {
            TestReadLock.withoutRead(() -> {
                HeadlessUIThread.run(() -> WriteCommandAction.runWriteCommandAction(project, runnable));
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

    protected String getTestDataPath() {
        return "src/test/java";
    }

    public Project getProject() {
        return myFixture.getProject();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // completion

    public void assertCompletionContains(FileType fileType, String configureByText, String... lookupStrings) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        completionContainsAssert(lookupStrings);
    }

    public void assertCompletionContains(String filename, String configureByText, String... lookupStrings) {
        myFixture.configureByText(filename, configureByText);
        myFixture.completeBasic();
        completionContainsAssert(lookupStrings);
    }

    public void assertAtTextCompletionContains(String findByText, String... lookupStrings) {
        PsiElement element = myFixture.findElementByText(findByText, PsiElement.class);
        assertNotNull("No element found by text: " + findByText, element);
        myFixture.configureFromExistingVirtualFile(myFixture.getFile().getVirtualFile());
        throw new UnsupportedOperationException("moving the caret of a configured file is not ported");
    }

    public void assertCompletionNotContains(FileType fileType, String configureByText, String... lookupStrings) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        assertCompletionResultsNotContain(lookupStrings);
    }

    public void assertCompletionNotContains(String filename, String configureByText, String... lookupStrings) {
        myFixture.configureByText(filename, configureByText);
        myFixture.completeBasic();
        assertCompletionResultsNotContain(lookupStrings);
    }

    public void assertCompletionResultsAreUnique(FileType fileType, String configureByText, String... lookupStrings) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        List<String> results = myFixture.getLookupElementStrings();
        if (results != null) {
            List<String> duplicates = results.stream()
                .filter(result -> Arrays.asList(lookupStrings).contains(result))
                .collect(Collectors.groupingBy(result -> result))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(java.util.Map.Entry::getKey)
                .toList();
            assertTrue("The following results are duplicated: " + String.join(", ", duplicates), duplicates.isEmpty());
        }
    }

    protected void assertCompletionResultsNotContain(String... lookupStrings) {
        assertTrue("Expected at least one forbidden completion", lookupStrings.length > 0);
        List<String> results = myFixture.getLookupElementStrings();
        assertNotNull("Expected completion lookup results", results);
        List<String> unexpected = Arrays.stream(lookupStrings).filter(results::contains).toList();
        assertTrue("Unexpected completion results: " + unexpected, unexpected.isEmpty());
    }

    public void assertCompletionIsEmpty(FileType fileType, String configureByText) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        List<String> lookupElements = myFixture.getLookupElementStrings();
        if (lookupElements != null && !lookupElements.isEmpty()) {
            fail("Failed that completion is empty: " + lookupElements);
        }
    }

    private void completionContainsAssert(String[] lookupStrings) {
        if (lookupStrings.length == 0) {
            fail("No lookup element given");
        }
        List<String> lookupElements = myFixture.getLookupElementStrings();
        if (lookupElements == null || lookupElements.isEmpty()) {
            fail(String.format("failed that empty completion contains %s", Arrays.toString(lookupStrings)));
        }
        for (String s : lookupStrings) {
            if (!lookupElements.contains(s)) {
                fail(String.format("failed that completion contains %s in %s", s, lookupElements));
            }
        }
    }

    public void assertCompletionLookupContainsPresentableItem(FileType fileType, String configureByText, LookupElementPresentationAssert.Assert presentationAssert) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        LookupElement[] lookupElements = myFixture.getLookupElements();
        if (lookupElements == null) {
            fail("failed to find lookup presentable on empty collection");
        }
        for (LookupElement lookupElement : lookupElements) {
            LookupElementPresentation presentation = new LookupElementPresentation();
            lookupElement.renderElement(presentation);
            if (presentationAssert.match(presentation)) {
                return;
            }
        }
        fail("failed to find lookup presentable");
    }

    public void assertCompletionLookupTailEquals(FileType fileType, String configureByText, String lookupString, String tailText) {
        myFixture.configureByText(fileType, configureByText);
        myFixture.completeBasic();
        for (LookupElement lookupElement : myFixture.getLookupElements()) {
            if (!lookupElement.getLookupString().equals(lookupString)) {
                continue;
            }
            LookupElementPresentation presentation = new LookupElementPresentation();
            lookupElement.renderElement(presentation);
            if (presentation.getTailText() == null) {
                fail(String.format("failed to check '%s'", lookupString));
            }
            if (!presentation.getTailText().equals(tailText)) {
                fail(String.format("failed that on '%s' '%s' is equal '%s'", lookupString, tailText, presentation.getTailText()));
            }
            return;
        }
        fail(String.format("failed to check '%s' because it's unknown", lookupString));
    }

    public void assertCompletionResultEquals(String filename, String complete, String result) {
        throw new UnsupportedOperationException("completing with the insert handler of a lookup element is not ported");
    }

    public void assertCompletionResultEquals(FileType fileType, String complete, String result) {
        throw new UnsupportedOperationException("completing with the insert handler of a lookup element is not ported");
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // navigation

    private List<PsiElement> gotoDeclarationTargets(PsiElement psiElement, int offset) {
        List<PsiElement> targets = new ArrayList<>();
        for (GotoDeclarationHandler handler : Application.get().getExtensionList(GotoDeclarationHandler.class)) {
            PsiElement[] handlerTargets = handler.getGotoDeclarationTargets(psiElement, offset, myFixture.getEditor());
            if (handlerTargets != null) {
                for (PsiElement target : handlerTargets) {
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
        }
        return targets;
    }

    private PsiElement elementAtCaret() {
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        if (psiElement == null) {
            fail("No element at caret found");
        }
        return psiElement;
    }

    public void assertNavigationContains(FileType fileType, String configureByText, String targetShortcut) {
        myFixture.configureByText(fileType, configureByText);
        assertNavigationContains(elementAtCaret(), targetShortcut);
    }

    public void assertNavigationContains(PsiElement psiElement, String targetShortcut) {
        String target = targetShortcut.startsWith("\\") ? targetShortcut : "\\" + targetShortcut;
        Set<String> classTargets = new HashSet<>();
        for (PsiElement gotoDeclarationTarget : gotoDeclarationTargets(psiElement, 0)) {
            if (gotoDeclarationTarget instanceof Method method) {
                String clName = method.getContainingClass().getPresentableFQN();
                classTargets.add((clName.startsWith("\\") ? clName : "\\" + clName) + "::" + method.getName());
            }
            else if (gotoDeclarationTarget instanceof Function function) {
                classTargets.add("\\" + function.getName());
            }
        }
        if (!classTargets.contains(target)) {
            fail(String.format("failed that PsiElement (%s) navigate to %s on %s", psiElement, target, classTargets));
        }
    }

    public void assertNavigationMatchWithParent(FileType fileType, String configureByText, IElementType elementType) {
        assertNavigationMatch(fileType, configureByText, PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(elementType)));
    }

    public void assertNavigationMatch(String filename, String configureByText, ElementPattern<?> pattern) {
        myFixture.configureByText(filename, configureByText);
        assertNavigationMatch(pattern);
    }

    public void assertNavigationMatch(FileType fileType, String configureByText, ElementPattern<?> pattern) {
        myFixture.configureByText(fileType, configureByText);
        assertNavigationMatch(pattern);
    }

    public void assertNavigationMatch(FileType fileType, String configureByText) {
        assertNavigationMatch(fileType, configureByText, PlatformPatterns.psiElement());
    }

    public void assertNavigationMatch(String filename, String configureByText) {
        assertNavigationMatch(filename, configureByText, PlatformPatterns.psiElement());
    }

    public void assertNavigationIsEmpty(FileType fileType, String configureByText) {
        myFixture.configureByText(fileType, configureByText);
        assertNavigationIsEmpty();
    }

    public void assertNavigationIsEmpty(String filename, String configureByText) {
        myFixture.configureByText(filename, configureByText);
        assertNavigationIsEmpty();
    }

    private void assertNavigationIsEmpty() {
        PsiElement psiElement = elementAtCaret();
        for (GotoDeclarationHandler handler : Application.get().getExtensionList(GotoDeclarationHandler.class)) {
            PsiElement[] targets = handler.getGotoDeclarationTargets(psiElement, 0, myFixture.getEditor());
            if (targets != null && targets.length > 0) {
                fail(String.format("failed that PsiElement (%s) navigate is empty; found target in '%s'", psiElement, handler.getClass()));
            }
        }
    }

    public void assertNavigationMatch(ElementPattern<?> pattern) {
        PsiElement psiElement = elementAtCaret();
        Set<String> targetStrings = new HashSet<>();
        for (PsiElement target : gotoDeclarationTargets(psiElement, myFixture.getCaretOffset())) {
            targetStrings.add(target.toString());
            if (pattern.accepts(target)) {
                return;
            }
        }
        fail(String.format("failed that PsiElement (%s) navigate matches one of %s", psiElement, targetStrings));
    }

    public void assertNavigationContainsFile(FileType fileType, String configureByText, String targetShortcut) {
        myFixture.configureByText(fileType, configureByText);
        PsiElement psiElement = elementAtCaret();
        for (PsiElement target : gotoDeclarationTargets(psiElement, 0)) {
            if (target instanceof PsiFile file && file.getVirtualFile().getUrl().endsWith(targetShortcut)) {
                return;
            }
        }
        fail(String.format("failed that PsiElement (%s) navigate to file %s", psiElement, targetShortcut));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // references

    public void assertPhpReferenceResolveTo(FileType fileType, String configureByText, ElementPattern<?> pattern) {
        PsiElement resolve = assertGetPhpReference(fileType, configureByText).resolve();
        if (!pattern.accepts(resolve)) {
            fail(String.format("failed pattern matches element of '%s'", resolve == null ? "null" : resolve.toString()));
        }
    }

    public void assertPhpReferenceNotResolveTo(FileType fileType, String configureByText, ElementPattern<?> pattern) {
        assertFalse(pattern.accepts(assertGetPhpReference(fileType, configureByText).resolve()));
    }

    public void assertPhpReferenceSignatureEquals(FileType fileType, String configureByText, String typeSignature) {
        assertEquals(typeSignature, assertGetPhpReference(fileType, configureByText).getSignature());
    }

    public void assertPhpReferenceSignatureContains(FileType fileType, String configureByText, String typeSignature) {
        assertTrue(assertGetPhpReference(fileType, configureByText).getSignature().contains(typeSignature));
    }

    private PhpReference assertGetPhpReference(FileType fileType, String configureByText) {
        myFixture.configureByText(fileType, configureByText);
        PhpReference reference = PsiTreeUtil.getParentOfType(elementAtCaret(), PhpReference.class);
        if (reference == null) {
            fail("Element is not PhpReference.");
        }
        return reference;
    }

    public void assertReferenceMatchOnParent(String file, String contents, ElementPattern<?> pattern) {
        myFixture.configureByText(file, contents);
        assertReferences(pattern, elementAtCaret().getParent());
    }

    public void assertReferenceMatchOnParent(FileType fileType, String contents, ElementPattern<?> pattern) {
        myFixture.configureByText(fileType, contents);
        assertReferences(pattern, elementAtCaret().getParent());
    }

    public void assertReferenceMatch(FileType fileType, String contents, ElementPattern<?> pattern) {
        myFixture.configureByText(fileType, contents);
        assertReferences(pattern, elementAtCaret());
    }

    private void assertReferences(ElementPattern<?> pattern, PsiElement psiElement) {
        for (PsiReference reference : psiElement.getReferences()) {
            if (pattern.accepts(reference.resolve())) {
                return;
            }
            if (reference instanceof PsiPolyVariantReference polyVariantReference) {
                for (ResolveResult resolveResult : polyVariantReference.multiResolve(true)) {
                    if (pattern.accepts(resolveResult.getElement())) {
                        return;
                    }
                }
            }
        }
        fail(String.format("Fail that '%s' match given pattern", psiElement));
    }

    protected PsiElement findElementAt(FileType fileType, String content) {
        myFixture.configureByText(fileType, content);
        return elementAtCaret();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // index

    public void assertIndexContains(ID<String, ?> id, String... keys) {
        assertIndex(id, false, keys);
    }

    public void assertIndexNotContains(ID<String, ?> id, String... keys) {
        assertIndex(id, true, keys);
    }

    public void assertIndex(ID<String, ?> id, boolean notCondition, String... keys) {
        for (String key : keys) {
            Collection<VirtualFile> virtualFiles = new ArrayList<>();
            FileBasedIndex.getInstance().getFilesWithKey(id, new HashSet<>(Collections.singletonList(key)), virtualFile -> {
                virtualFiles.add(virtualFile);
                return true;
            }, GlobalSearchScope.allScope(getProject()));

            if (notCondition && !virtualFiles.isEmpty()) {
                fail(String.format("Fail that ID '%s' not contains '%s'", id, key));
            }
            else if (!notCondition && virtualFiles.isEmpty()) {
                fail(String.format("Fail that ID '%s' contains '%s'", id, key));
            }
        }
    }

    public void assertIndexContainsKeyWithValue(ID<String, String> id, String key, String value) {
        assertContainsElements(FileBasedIndex.getInstance().getValues(id, key, GlobalSearchScope.allScope(getProject())), value);
    }

    public <T> void assertIndexContainsKeyWithValue(ID<String, T> id, String key, IndexValue.Assert<T> tAssert) {
        List<T> values = FileBasedIndex.getInstance().getValues(id, key, GlobalSearchScope.allScope(getProject()));
        for (T t : values) {
            if (tAssert.match(t)) {
                return;
            }
        }
        fail(String.format("Fail that Key '%s' matches on of '%s' values", key, values.size()));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // inspections and intentions

    public void assertLocalInspectionContains(Class<? extends LocalInspectionTool> inspectionClass, String filename, String content, String contains) {
        Set<String> matches = new HashSet<>();
        int caretOffset = configureForInspection(filename, content);
        for (ProblemDescriptor result : getLocalInspections(inspectionClass)) {
            TextRange textRange = result.getPsiElement().getTextRange();
            String description = result.getDescriptionTemplate().get();
            if (textRange.contains(caretOffset) && description.equals(contains)) {
                return;
            }
            matches.add(description);
        }
        fail(String.format("Fail matches '%s' with one of %s", contains, matches));
    }

    public void assertLocalInspectionNotContains(Class<? extends LocalInspectionTool> inspectionClass, String filename, String content, String contains) {
        int caretOffset = configureForInspection(filename, content);
        for (ProblemDescriptor result : getLocalInspections(inspectionClass)) {
            TextRange textRange = result.getPsiElement().getTextRange();
            if (textRange.contains(caretOffset) && result.getDescriptionTemplate().get().contains(contains)) {
                fail(String.format("Fail inspection not contains '%s'", contains));
            }
        }
    }

    private int configureForInspection(String filename, String content) {
        myFixture.configureByText(filename, content);
        int caretOffset = myFixture.getCaretOffset();
        if (caretOffset <= 0) {
            fail("Please provide <caret> tag");
        }
        return caretOffset;
    }

    private List<ProblemDescriptor> getLocalInspections(Class<? extends LocalInspectionTool> inspectionClass) {
        PsiFile psiFile = myFixture.getFile();
        ProblemsHolder problemsHolder = InspectionManager.getInstance(getProject()).createProblemsHolder(psiFile, false);
        InspectionTool tool = Application.get().getExtensionPoint(InspectionTool.class).findExtension(inspectionClass);
        if (tool == null) {
            fail("Inspection is not registered: " + inspectionClass.getName());
        }
        LocalInspectionTool inspection = (LocalInspectionTool) tool;
        InspectionToolState<?> stateProvider = inspection.createStateProvider();
        Object state = stateProvider == null ? null : stateProvider.getState();
        PsiElementVisitor visitor = inspection.buildVisitor(problemsHolder, false, null, state);
        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                visitor.visitElement(element);
                super.visitElement(element);
            }
        });
        visitor.visitFile(psiFile);
        return problemsHolder.getResults();
    }

    public void assertIntentionIsAvailable(FileType fileType, String configureByText, String intentionText) {
        myFixture.configureByText(fileType, configureByText);
        PsiElement psiElement = elementAtCaret();
        Set<String> items = new HashSet<>();
        for (IntentionAction intentionAction : IntentionManager.getInstance().getIntentionActions()) {
            if (!intentionAction.isAvailable(getProject(), myFixture.getEditor(), psiElement.getContainingFile())) {
                continue;
            }
            String text = intentionAction.getText().get();
            items.add(text);
            if (text.equals(intentionText)) {
                return;
            }
        }
        fail(String.format("Fail intention action '%s' is available in element '%s' with '%s'", intentionText, psiElement.getText(), items));
    }

    protected void createDummyFiles(String... files) {
        for (String file : files) {
            myFixture.addFileToProject(file, "");
        }
    }

    protected void createFileInProjectRoot(String relativePath, String content) {
        myFixture.addFileToProjectRoot(relativePath, content == null ? "" : content);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // line markers

    public void assertLineMarker(PsiElement psiElement, LineMarker.Assert assertMatch) {
        List<PsiElement> elements = collectPsiElementsRecursive(psiElement);
        for (LineMarkerProvider lineMarkerProvider : LineMarkerProvider.forLanguage(psiElement.getLanguage())) {
            Collection<LineMarkerInfo> lineMarkerInfos = new ArrayList<>();
            lineMarkerProvider.collectSlowLineMarkers(elements, lineMarkerInfos);
            for (LineMarkerInfo lineMarkerInfo : lineMarkerInfos) {
                if (assertMatch.match(lineMarkerInfo)) {
                    return;
                }
            }
        }
        fail(String.format("Fail that '%s' matches on of '%s' PsiElements", assertMatch.getClass(), elements.size()));
    }

    public void assertLineMarkerIsEmpty(PsiElement psiElement) {
        List<PsiElement> elements = collectPsiElementsRecursive(psiElement);
        for (LineMarkerProvider lineMarkerProvider : LineMarkerProvider.forLanguage(psiElement.getLanguage())) {
            Collection<LineMarkerInfo> lineMarkerInfos = new ArrayList<>();
            lineMarkerProvider.collectSlowLineMarkers(elements, lineMarkerInfos);
            if (!lineMarkerInfos.isEmpty()) {
                fail(String.format("Fail that line marker is empty because it matches '%s'", lineMarkerProvider.getClass()));
            }
        }
    }

    private List<PsiElement> collectPsiElementsRecursive(PsiElement psiElement) {
        List<PsiElement> elements = new ArrayList<>();
        elements.add(psiElement.getContainingFile());
        psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                elements.add(element);
                super.visitElement(element);
            }
        });
        return elements;
    }

    public static class IndexValue {
        public interface Assert<T> {
            boolean match(T value);
        }
    }

    public static class LineMarker {
        public interface Assert {
            boolean match(LineMarkerInfo markerInfo);
        }

        public static class ToolTipEqualsAssert implements Assert {
            private final String myToolTip;

            public ToolTipEqualsAssert(String toolTip) {
                myToolTip = toolTip;
            }

            @Override
            public boolean match(LineMarkerInfo markerInfo) {
                return myToolTip.equals(markerInfo.getLineMarkerTooltip());
            }
        }

        public static class TargetAcceptsPattern implements Assert {
            private final String myToolTip;
            private final ElementPattern<? extends PsiElement> myPattern;

            public TargetAcceptsPattern(String toolTip, ElementPattern<? extends PsiElement> pattern) {
                myToolTip = toolTip;
                myPattern = pattern;
            }

            @Override
            public boolean match(LineMarkerInfo markerInfo) {
                if (!myToolTip.equals(markerInfo.getLineMarkerTooltip()) || !(markerInfo instanceof RelatedItemLineMarkerInfo<?> relatedInfo)) {
                    return false;
                }
                for (GotoRelatedItem item : relatedInfo.createGotoRelatedItems()) {
                    if (myPattern.accepts(item.getElement())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public static class LookupElementPresentationAssert {
        public interface Assert {
            boolean match(LookupElementPresentation lookupElement);
        }
    }
}
