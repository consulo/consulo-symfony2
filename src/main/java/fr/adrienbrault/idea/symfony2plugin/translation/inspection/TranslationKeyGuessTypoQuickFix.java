package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TranslationKeyGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    private final String missingTranslationKey;

    private final String translationDomain;

    public TranslationKeyGuessTypoQuickFix(String missingTranslationKey, String translationDomain) {
        this.missingTranslationKey = missingTranslationKey;
        this.translationDomain = translationDomain;
    }

    @Override
    protected String getSuggestionLabel() {
        return "Translation Key";
    }

    @Override
    protected List<String> getSimilarItems(Project project) {
        Set<String> translationKeys = TranslationUtil.getTranslationLookupElementsOnDomain(project, translationDomain).stream()
            .map(LookupElement::getLookupString)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return SimilarSuggestionUtil.findSimilarString(missingTranslationKey, translationKeys);
    }
}
