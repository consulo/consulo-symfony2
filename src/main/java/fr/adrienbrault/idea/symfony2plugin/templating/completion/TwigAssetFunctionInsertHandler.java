package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAssetFunctionInsertHandler implements InsertHandler<AssetLookupElement> {

    private static final TwigAssetFunctionInsertHandler instance = new TwigAssetFunctionInsertHandler();

    public void handleInsert(InsertionContext context, AssetLookupElement lookupElement) {
        context.getDocument().insertString(context.getStartOffset(), "{{ asset('");
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "') }}");
    }

    public static TwigAssetFunctionInsertHandler getInstance(){
        return instance;
    }

}