package fr.adrienbrault.idea.symfony2plugin.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiMultiReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceRange;

/**
 * Completes the names of the {@link SymfonyCompletionReference}s at the caret, in whatever language they are
 * contributed to: PHP, Twig, YAML, XML.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
@ExtensionImpl
public class SymfonyReferenceCompletionContributor extends CompletionContributor {
    @RequiredReadAction
    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }
        int offset = parameters.getOffset();
        PsiReference reference = parameters.getPosition().getContainingFile().findReferenceAt(offset);
        if (reference instanceof PsiMultiReference multiReference) {
            for (PsiReference child : multiReference.getReferences()) {
                complete(child, offset, result);
            }
        }
        else if (reference != null) {
            complete(reference, offset, result);
        }
    }

    @RequiredReadAction
    private static void complete(PsiReference reference, int offset, CompletionResultSet result) {
        if (!(reference instanceof SymfonyCompletionReference completionReference)) {
            return;
        }
        PsiElement element = reference.getElement();
        int offsetInElement = offset - element.getTextRange().getStartOffset();
        if (!ReferenceRange.containsOffsetInElement(reference, offsetInElement)) {
            return;
        }
        String prefix = element.getText().substring(reference.getRangeInElement().getStartOffset(), offsetInElement);
        CompletionResultSet prefixed = result.withPrefixMatcher(prefix);
        for (Object variant : completionReference.getCompletionVariants()) {
            if (variant instanceof LookupElement lookupElement) {
                prefixed.addElement(lookupElement);
            }
            else if (variant instanceof String name) {
                prefixed.addElement(LookupElementBuilder.create(name));
            }
        }
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
