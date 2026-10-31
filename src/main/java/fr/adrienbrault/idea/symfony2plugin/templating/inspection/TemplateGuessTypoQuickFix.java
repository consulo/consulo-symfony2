package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import consulo.project.Project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    private final String missingTemplateName;

    public TemplateGuessTypoQuickFix(String missingTemplateName) {
        this.missingTemplateName = missingTemplateName;
    }

    @Override
    protected String getSuggestionLabel() {
        return "Template";
    }

    @Override
    protected List<String> getSimilarItems(Project project) {
        String strippedInput = stripTemplateFormatAndExtensionLowered(missingTemplateName);

        Map<String, String> strippedToOriginal = new HashMap<>();
        Set<String> strippedNames = new HashSet<>();

        for (String template : TwigUtil.getTemplateMap(project, true).keySet()) {
            String stripped = stripTemplateFormatAndExtensionLowered(template);
            strippedToOriginal.put(stripped, template);
            strippedNames.add(stripped);
        }

        return SimilarSuggestionUtil.findSimilarString(strippedInput, strippedNames).stream()
            .map(strippedToOriginal::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static String stripTemplateFormatAndExtensionLowered(String templateNameIfMissing) {
        return templateNameIfMissing
            .toLowerCase()
            .replaceAll("\\.[^.]*$", "").replaceAll("\\.[^.]*$", "");
    }
}
