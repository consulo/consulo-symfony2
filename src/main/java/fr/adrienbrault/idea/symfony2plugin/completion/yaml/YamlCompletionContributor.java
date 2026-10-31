package fr.adrienbrault.idea.symfony2plugin.completion.yaml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.completion.YamlKeywordsCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.YamlTagCompletionProvider;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
@ExtensionImpl
public class YamlCompletionContributor extends CompletionContributor {
    public YamlCompletionContributor() {
        // config:
        //   key: !<caret>
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSingleLineTextOrTag(),
            new YamlTagCompletionProvider()
        );

        // config:
        //   key: <caret>
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSingleLineText(),
            new YamlKeywordsCompletionProvider()
        );
    }

    @Override
    public boolean invokeAutoPopup(PsiElement position, char typeChar) {
        // Only for Yaml tag places (scalar values)
        //   key: !<caret>
        if (!YamlElementPatternHelper.getSingleLineTextOrTag().accepts(position)
            && !(position.getPrevSibling() instanceof YAMLKeyValue)
            && !(position.getParent() instanceof YAMLSequenceItem)
            && !(position.getParent() instanceof YAMLSequence)
        ) {
            return super.invokeAutoPopup(position, typeChar);
        }

        return typeChar == '!';
    }

    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
