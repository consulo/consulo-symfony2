package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.YamlServiceTagIntention", fileExtensions = "yaml", categories = "Symfony")
public class YamlServiceTagIntention extends PsiElementBaseIntentionAction {
    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>yaml</language>
        if (!psiElement.getLanguage().isKindOf(YAMLLanguage.INSTANCE)) {
            return false;
        }

        if (psiElement.getContainingFile().getFileType() != YAMLFileType.YML || !Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        return YamlHelper.findServiceInContext(psiElement) != null;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        YAMLKeyValue serviceKeyValue = YamlHelper.findServiceInContext(psiElement);
        if (serviceKeyValue == null) {
            return;
        }

        Pair<PhpClass, Set<String>> invoke = collectServiceTags(project, serviceKeyValue);
        if (invoke == null) {
            return;
        }

        Set<String> phpClassServiceTags = invoke.second;
        if (phpClassServiceTags.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "Ops, no possible Tag found");
            return;
        }

        PhpClass resolvedClassDefinition = invoke.first;

        int appendEndOffset = -1;
        String insertString = null;

        YAMLKeyValue argumentsKeyValue = YamlHelper.getYamlKeyValue(serviceKeyValue, "tags");
        if (argumentsKeyValue == null) {
            // we dont an "tags" key so we need to create one

            String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(serviceKeyValue));

            List<String> yamlSequences = new ArrayList<>();
            for (String item : phpClassServiceTags) {
                ServiceTag serviceTag = new ServiceTag(resolvedClassDefinition, item);
                ServiceUtil.decorateServiceTag(serviceTag);
                yamlSequences.add(indent + " " + serviceTag.toYamlString());
            }

            appendEndOffset = serviceKeyValue.getTextRange().getEndOffset();

            String eol = TranslationInsertUtil.findLineSeparator(serviceKeyValue);
            insertString = eol + indent + "tags:" + eol + StringUtils.join(yamlSequences, eol);
        }
        else {
            // we found a "tags" key so update
            YAMLValue value = argumentsKeyValue.getValue();
            if (!(value instanceof YAMLCompoundValue)) {
                IdeHelper.showErrorHintIfAvailable(editor, "Sry, not supported tags definition");
                return;
            }

            PsiElement firstChild = value.getFirstChild();
            if (firstChild instanceof YAMLSequence) {
                String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(argumentsKeyValue));

                List<String> yamlSequences = new ArrayList<>();
                for (String item : phpClassServiceTags) {
                    ServiceTag serviceTag = new ServiceTag(resolvedClassDefinition, item);
                    ServiceUtil.decorateServiceTag(serviceTag);
                    yamlSequences.add(indent + serviceTag.toYamlString());
                }

                appendEndOffset = argumentsKeyValue.getTextRange().getEndOffset();

                String eol = TranslationInsertUtil.findLineSeparator(argumentsKeyValue);
                insertString = eol + StringUtils.join(yamlSequences, eol);
            }
        }

        if (appendEndOffset == -1) {
            IdeHelper.showErrorHintIfAvailable(editor, "Sry, not supported service definition");
            return;
        }

        PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        Document document = manager.getDocument(serviceKeyValue.getContainingFile());
        if (document == null) {
            return;
        }

        document.insertString(appendEndOffset, insertString);
        manager.doPostponedOperationsAndUnblockDocument(document);
        manager.commitDocument(document);
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add Tags");
    }

    private static @Nullable Pair<PhpClass, Set<String>> collectServiceTags(Project project, YAMLKeyValue serviceKeyValue) {
        String className = YamlHelper.getYamlKeyValueAsString(serviceKeyValue, "class");
        if (className == null || StringUtils.isBlank(className)) {
            PsiElement key = serviceKeyValue.getKey();
            if (key != null) {
                String text = key.getText();
                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text)) {
                    className = text;
                }
            }
        }

        if (className == null) {
            return null;
        }

        PhpClass resolvedClassDefinition = ServiceUtil.getResolvedClassDefinition(
            project,
            className,
            new ContainerCollectionResolver.LazyServiceCollector(project)
        );
        if (resolvedClassDefinition == null) {
            return null;
        }

        Set<String> phpClassServiceTags = ServiceUtil.getPhpClassServiceTags(resolvedClassDefinition);

        Set<String> strings = YamlHelper.collectServiceTags(serviceKeyValue);
        if (!strings.isEmpty()) {
            for (String string : strings) {
                phpClassServiceTags.remove(string);
            }
        }

        return Pair.create(resolvedClassDefinition, phpClassServiceTags);
    }
}
