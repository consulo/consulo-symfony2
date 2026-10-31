package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathFunctionInsertHandler implements InsertHandler<RouteLookupElement> {

    private static final TwigPathFunctionInsertHandler instance = new TwigPathFunctionInsertHandler();

    public void handleInsert(InsertionContext context, RouteLookupElement lookupElement) {
        context.getDocument().insertString(context.getStartOffset(), "{{ path('");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");

        Set<String> vars = lookupElement.getRoute().getVariables();
        if(!vars.isEmpty()) {
            List<String> items = new ArrayList<>();
            for(String var: vars) {
                items.add(String.format("'%s': '%s'", var, "x"));
            }
            stringBuilder.append(", {");
            stringBuilder.append(StringUtils.join(items, ", "));
            stringBuilder.append("}");
        }

        stringBuilder.append(") }}");
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), stringBuilder.toString());
    }

    public static TwigPathFunctionInsertHandler getInstance(){
        return instance;
    }

}