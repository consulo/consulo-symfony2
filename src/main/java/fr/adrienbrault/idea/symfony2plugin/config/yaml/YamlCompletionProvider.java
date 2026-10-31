package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionProvider implements CompletionProvider {

    @Nullable
    private List<LookupElement> lookupList;

    @Nullable
    private Map<String, String> lookupMap;

    public YamlCompletionProvider(@Nullable List<LookupElement> lookups) {
        this.lookupList = lookups;
    }

    public YamlCompletionProvider(@Nullable Map<String, String> lookups) {
        this.lookupMap = lookups;
    }

    public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet resultSet) {
        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        if(this.lookupList != null) {
            resultSet.addAllElements(this.lookupList);
        } else if(lookupMap != null) {
            for (Map.Entry<String, String> lookup : lookupMap.entrySet()) {
                LookupElementBuilder lookupElement = LookupElementBuilder.create(lookup.getKey()).withTypeText(lookup.getValue(), true).withIcon(Symfony2Icons.SYMFONY);
                if(lookup.getValue() != null && lookup.getValue().contains("deprecated")) {
                    lookupElement = lookupElement.withStrikeoutness(true);
                }

                resultSet.addElement(lookupElement);
            }
        }
    }
}
