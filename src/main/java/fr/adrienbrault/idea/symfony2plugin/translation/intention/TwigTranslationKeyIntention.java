package fr.adrienbrault.idea.symfony2plugin.translation.intention;

import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.Pair;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.TwigTranslationKeyIntention", fileExtensions = {"twig", "yaml"}, categories = "Symfony")
public final class TwigTranslationKeyIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, @Nullable Editor editor, PsiElement psiElement) {
        Pair<String, String> pair = getKeyAndDomain(psiElement);
        if (pair == null) {
            return;
        }

        new TranslationKeyIntentionAndQuickFixAction(pair.getFirst(), pair.getSecond(), new MyKeyDomainNotExistingCollector())
            .invoke(project, editor, psiElement.getContainingFile());
    }

    @Override
    public boolean isAvailable(Project project, @Nullable Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>Twig</language>
        if (!psiElement.getLanguage().isKindOf(TwigLanguage.INSTANCE)) {
            return false;
        }

        return getKeyAndDomain(psiElement) != null;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: create translation key");
    }

    private @Nullable Pair<String, String> getKeyAndDomain(PsiElement psiElement) {
        if (!TwigPattern.getTranslationKeyPattern("trans", "transchoice").accepts(psiElement)) {
            return null;
        }

        String key = psiElement.getText();
        if (StringUtils.isBlank(key)) {
            return null;
        }

        // get domain on file scope or method parameter
        String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

        // inspection will take care of complete unknown key
        if (!TranslationUtil.hasTranslationKey(psiElement.getProject(), key, domainName)) {
            return null;
        }

        return Pair.create(key, domainName);
    }

    /**
     * Collect all domain files that are not providing the given key
     * Known VirtualFiles are filtered out based on the index
     */
    private static final class MyKeyDomainNotExistingCollector implements TranslationKeyIntentionAndQuickFixAction.DomainCollector {
        @Override
        public Collection<PsiFile> collect(Project project, String key, String domain) {
            return TranslationUtil.getDomainPsiFiles(project, domain).stream()
                .filter(psiFile -> !isDomainAndKeyInPsi(psiFile, key, domain))
                .collect(Collectors.toList());
        }

        private boolean isDomainAndKeyInPsi(PsiFile psiFile, String key, String domain) {
            List<Set<String>> values = FileBasedIndex.getInstance()
                .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.fileScope(psiFile));

            for (Set<String> value : values) {
                if (value.contains(key)) {
                    return true;
                }
            }

            return false;
        }
    }
}
