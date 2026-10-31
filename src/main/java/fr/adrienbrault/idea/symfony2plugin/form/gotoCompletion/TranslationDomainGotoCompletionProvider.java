package fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Global translation domain
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainGotoCompletionProvider extends GotoCompletionProvider {

    public TranslationDomainGotoCompletionProvider(PsiElement element) {
        super(element);
    }

    @Override
    public Collection<LookupElement> getLookupElements() {
        return TranslationUtil.getTranslationDomainLookupElements(getProject());
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String stringLiteralValue = GotoCompletionUtil.getStringLiteralValue(element);
        if(stringLiteralValue == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(TranslationUtil.getDomainPsiFiles(getProject(), stringLiteralValue));
    }
}
