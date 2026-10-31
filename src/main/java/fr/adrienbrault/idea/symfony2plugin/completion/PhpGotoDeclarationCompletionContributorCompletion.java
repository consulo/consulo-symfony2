package fr.adrienbrault.idea.symfony2plugin.completion;

import com.jetbrains.php.lang.PhpLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * Moved from nested class PhpGotoDeclarationCompletionContributor.Completion
 * (Consulo cannot register nested extension classes)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpGotoDeclarationCompletionContributorCompletion extends CompletionContributor {
    public PhpGotoDeclarationCompletionContributorCompletion() {
        // $httpClient->request('', '', ['<caret>' => '']);
        extend(CompletionType.BASIC, PhpElementsUtil.getParameterListArrayValuePattern(), new PhpGotoDeclarationCompletionContributor.HttpClientCompletionProvider());
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
