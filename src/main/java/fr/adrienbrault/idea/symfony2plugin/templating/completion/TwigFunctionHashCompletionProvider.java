package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil.ShapeKey;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Completes hash keys and literal string values from the PHPDoc array shape of the PHP parameter behind a Twig function.
 * <p>
 * Examples:
 * <ul>
 *   <li><code>{{ button({ <caret> }) }}</code></li>
 *   <li><code>{{ button({ size: '<caret>' }) }}</code></li>
 * </ul>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class TwigFunctionHashCompletionProvider implements CompletionProvider {
    private static final Pattern TWIG_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();
        if (!Symfony2ProjectComponent.isEnabled(position)) {
            return;
        }

        FunctionHash hash = TwigFunctionHashParser.parse(position);
        if (hash == null) {
            return;
        }

        Project project = position.getProject();
        TwigExtension twigExtension = TwigExtensionParser.getFunctions(project).get(hash.functionName());
        if (twigExtension == null) {
            return;
        }

        int parameterIndex = getParameterIndex(twigExtension, hash.argumentIndex());

        Map<String, LookupElement> lookupElements = new LinkedHashMap<>();
        for (PhpNamedElement element : PhpElementsUtil.getPsiElementsBySignature(project, twigExtension.getSignature())) {
            if (!(element instanceof Function function)) {
                continue;
            }

            for (LookupElement lookupElement : createLookupElements(function, parameterIndex, hash)) {
                lookupElements.putIfAbsent(lookupElement.getLookupString(), lookupElement);
            }
        }

        CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(parameters, resultSet);
        completionResultSet.addAllElements(lookupElements.values());
    }

    private static int getParameterIndex(TwigExtension twigExtension, int argumentIndex) {
        int parameterIndex = argumentIndex;
        if ("true".equals(twigExtension.getOption("needs_environment"))) {
            parameterIndex++;
        }

        if ("true".equals(twigExtension.getOption("needs_context"))) {
            parameterIndex++;
        }

        return parameterIndex;
    }

    private static List<LookupElement> createLookupElements(Function function, int parameterIndex, FunctionHash hash) {
        List<LookupElement> lookupElements = new ArrayList<>();

        String valueKey = hash.valueKey();
        if (valueKey != null) {
            for (String value : PhpDocArrayShapeUtil.getParameterLiteralValues(function, parameterIndex, hash.keyPath(), valueKey)) {
                lookupElements.add(LookupElementBuilder.create(value).withIcon(Symfony2Icons.SYMFONY));
            }

            return lookupElements;
        }

        for (ShapeKey key : PhpDocArrayShapeUtil.getParameterKeys(function, parameterIndex, hash.keyPath())) {
            lookupElements.add(createKeyLookupElement(key, hash.quoted()));
        }

        return lookupElements;
    }

    private static LookupElement createKeyLookupElement(ShapeKey key, boolean quoted) {
        String name = key.name();
        boolean needsQuotes = !quoted && !TWIG_NAME.matcher(name).matches();
        String lookupString = needsQuotes ? "'" + name + "'" : name;

        return LookupElementBuilder.create(lookupString)
            .withLookupString(name)
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(!key.optional());
    }
}
