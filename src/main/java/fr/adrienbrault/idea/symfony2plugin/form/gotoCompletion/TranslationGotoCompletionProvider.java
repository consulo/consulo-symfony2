package fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Global translation domain
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationGotoCompletionProvider extends GotoCompletionProvider {

    private final String domain;

    public TranslationGotoCompletionProvider(PsiElement element, String domain) {
        super(element);
        this.domain = domain;
    }

    @Override
    public Collection<LookupElement> getLookupElements() {
        return TranslationUtil.getTranslationLookupElementsOnDomain(getProject(), domain);
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String contents = GotoCompletionUtil.getStringLiteralValue(element);
        if(contents == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(TranslationUtil.getTranslationPsiElements(getProject(), contents, domain));
    }
}