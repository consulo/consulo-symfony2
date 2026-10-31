package fr.adrienbrault.idea.symfony2plugin.util.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlKeywordsCompletionProvider implements CompletionProvider {
    private final static Map<String, String> YAML_KEYWORDS = new HashMap<>() {{
        put("~", "null");
        put("null", "null");
        put("true", "bool");
        put("false", "bool");
        put(".inf", "double");
    }};

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
        PsiElement psiElement = parameters.getPosition();

        if (psiElement instanceof LeafPsiElement) {
            // Don't complete after tag (at end of line)
            // key: !my_tag <caret>\n
            if (YamlHelper.isElementAfterYamlTag(psiElement)) {
                return;
            }

            // Don't complete after End Of Line:
            // key: foo\n
            // <caret>
            if (YamlHelper.isElementAfterEol(psiElement)) {
                return;
            }

            String prefix = psiElement.getText();

            if (prefix.contains(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)) {
                prefix = prefix.substring(0, prefix.indexOf(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED));
            }

            result = result.withPrefixMatcher(prefix);
        }

        for (Map.Entry<String, String> entry : YAML_KEYWORDS.entrySet()) {
            String yamlKeyword = entry.getKey();
            String yamlType = entry.getValue();

            LookupElementBuilder lookupElement = LookupElementBuilder.create(yamlKeyword)
                    .withTypeText(yamlType);

            result.addElement(lookupElement);
        }
    }
}
