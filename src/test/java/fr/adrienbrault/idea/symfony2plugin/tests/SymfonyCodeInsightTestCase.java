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

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionType;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiFileFactory;
import consulo.util.lang.LocalTimeCounter;
import consulo.it.AllowLogError;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.HeadlessUIThread;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.php.module.extension.PhpMutableModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyMutableModuleExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The base of the Symfony plugin tests, in the shape of upstream's {@code SymfonyLightCodeInsightFixtureTestCase}:
 * a headless project with one module that has the PHP and the Symfony module extensions enabled.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
@ExtendWith(HeadlessProjectExtension.class)
// TODO the XML plugin outside the IDE: its standard schemas are looked up with a leading slash on a class loader
//  (ExternalResource), and XmlReferenceContributor needs CommentsReferenceContributor of ide-impl
@AllowLogError({"consulo.xml.impl.internal.ExternalResource", "consulo.language.impl.internal.psi.resolve.reference.ReferenceProvidersRegistryImpl"})
public abstract class SymfonyCodeInsightTestCase {
    protected CodeInsightTestFixture myFixture;

    @BeforeEach
    public void setUpSymfony(CodeInsightTestFixture fixture) throws Exception {
        myFixture = fixture;
        WriteAction.run(() -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(fixture.getModule()).getModifiableModel();
            PhpMutableModuleExtension<?> php = model.getExtensionWithoutCheck(PhpMutableModuleExtension.class);
            assertThat(php).as("PHP module extension").isNotNull();
            php.setEnabled(true);
            SymfonyMutableModuleExtension symfony = model.getExtensionWithoutCheck(SymfonyMutableModuleExtension.class);
            assertThat(symfony).as("Symfony module extension").isNotNull();
            symfony.setEnabled(true);
            model.commit();
        });
        HeadlessProjects.awaitIdle(fixture.getProject());
    }

    /**
     * Where Ctrl+B goes from the caret: the targets of the {@link GotoDeclarationHandler}s and of the references
     */
    protected List<PsiElement> getNavigationTargets(String fileName, String text) throws Exception {
        PsiFile file = myFixture.configureByText(fileName, text);
        int offset = myFixture.getExpectedHighlightingData().getCaretOffset();
        return ReadAction.compute(() -> {
            Collection<PsiElement> targets = new LinkedHashSet<>();
            PsiElement element = file.findElementAt(offset);
            if (element != null) {
                for (GotoDeclarationHandler handler : GotoDeclarationHandler.EP_NAME.getExtensionList()) {
                    PsiElement[] handlerTargets = handler.getGotoDeclarationTargets(element, offset, null);
                    if (handlerTargets != null) {
                        for (PsiElement target : handlerTargets) {
                            if (target != null) {
                                targets.add(target);
                            }
                        }
                    }
                }
            }

            PsiReference reference = file.findReferenceAt(offset);
            if (reference instanceof PsiPolyVariantReference polyVariantReference) {
                for (ResolveResult result : polyVariantReference.multiResolve(false)) {
                    if (result.getElement() != null) {
                        targets.add(result.getElement());
                    }
                }
            }
            else if (reference != null) {
                PsiElement resolved = reference.resolve();
                if (resolved != null) {
                    targets.add(resolved);
                }
            }
            return new ArrayList<>(targets);
        });
    }

    protected void assertNavigationMatch(String fileName, String text, Predicate<PsiElement> target, String description) throws Exception {
        List<PsiElement> targets = getNavigationTargets(fileName, text);
        assertThat(ReadAction.compute(() -> targets.stream().anyMatch(target)))
            .as("navigation to " + description + " in " + ReadAction.compute(() -> targets.stream().map(PsiElement::toString).toList()))
            .isTrue();
    }

    /**
     * The lookup strings of a basic completion at the caret, computed like the IDE does: on a copy of the file with the
     * dummy identifier at the caret
     */
    protected List<String> getCompletionLookupStrings(String fileName, String text) throws Exception {
        PsiFile file = myFixture.configureByText(fileName, text);
        int offset = myFixture.getExpectedHighlightingData().getCaretOffset();
        Document document = myFixture.getDocument();
        // editors live on the UI thread
        Editor editor = HeadlessUIThread.compute(() -> EditorFactory.getInstance().createEditor(document, myFixture.getProject()));
        try {
            return ReadAction.compute(() -> {
                String original = file.getText();
                String copyText = original.substring(0, offset) + CompletionInitializationContext.DUMMY_IDENTIFIER + original.substring(offset);
                PsiFile copy = PsiFileFactory.getInstance(myFixture.getProject())
                    .createFileFromText(file.getName(), file.getFileType(), copyText, LocalTimeCounter.currentTime(), false, true);
                ((PsiFileImpl) copy).setOriginalFile(file);
                PsiElement position = copy.findElementAt(offset);
                assertThat(position).as("element at the caret of the completion copy").isNotNull();

                CompletionParameters parameters = new CompletionParameters(position, file, CompletionType.BASIC, offset, 1, editor, () -> false);
                List<String> lookups = new ArrayList<>();
                CompletionService.getCompletionService().performCompletion(parameters, result -> lookups.addAll(result.getLookupElement().getAllLookupStrings()));
                return lookups;
            });
        }
        finally {
            HeadlessUIThread.run(() -> EditorFactory.getInstance().releaseEditor(editor));
        }
    }

    protected void assertCompletionContains(String fileName, String text, String... lookupStrings) throws Exception {
        assertThat(getCompletionLookupStrings(fileName, text)).as("completion in " + fileName).contains(lookupStrings);
    }

    protected void assertCompletionNotContains(String fileName, String text, String... lookupStrings) throws Exception {
        assertThat(getCompletionLookupStrings(fileName, text)).as("completion in " + fileName).doesNotContain(lookupStrings);
    }
}
