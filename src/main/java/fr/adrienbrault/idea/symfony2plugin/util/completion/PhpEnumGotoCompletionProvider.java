package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.PhpVariantsUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * key: !php/enum Foo\Bar::BAZ
 */
public class PhpEnumGotoCompletionProvider extends GotoCompletionProvider {

    private static final String SCOPE_OPERATOR = "::";

    public PhpEnumGotoCompletionProvider(PsiElement element) {
        super(element);
    }

    @Override
    public void getLookupElements(GotoCompletionProviderLookupArguments arguments) {
        CompletionResultSet resultSet = arguments.getResultSet();

        String elementText = getElement().getText();
        int scopeOperatorPos = elementText.indexOf(SCOPE_OPERATOR);
        int cursorPos = elementText.indexOf(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED);

        // Enum cases:  !php/enum Foo::<caret>
        if (scopeOperatorPos > -1 && scopeOperatorPos < cursorPos) {
            String prefix = elementText.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
            // reset the prefix matcher, starting after ::
            resultSet = resultSet.withPrefixMatcher(prefix.substring(scopeOperatorPos + 2));

            // same FQN may be indexed multiple times; only enums provide cases
            for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(this.getProject(), prefix.substring(0, scopeOperatorPos))) {
                if (phpClass.isEnum()) {
                    resultSet.addAllElements(PhpVariantsUtil.getLookupItems(phpClass.getEnumCases(), false, null));
                }
            }
            return;
        }

        // Enums:  !php/enum Foo
        PhpIndex phpIndex = PhpIndex.getInstance(this.getProject());
        Collection<LookupElement> elements = new ArrayList<>();
        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {
            for (PhpClass phpClass : phpIndex.getClassesByName(className)) {
                if (phpClass.isEnum()) {
                    PhpLookupElement lookupElement = new PhpLookupElement(phpClass);
                    lookupElement.handler = PhpConstGotoCompletionProvider.PhpClassWithScopeOperatorInsertHandler.getInstance();
                    elements.add(lookupElement);
                }
            }
        }

        arguments.addAllElements(elements);
    }
}
