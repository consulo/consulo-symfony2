package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DoctrineYamlGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(PlatformPatterns.or(
            YamlElementPatternHelper.getOrmSingleLineScalarKey("repositoryClass")
        ), ClassGotoCompletionProvider::new);
    }

    private static class ClassGotoCompletionProvider extends GotoCompletionProvider {
        private ClassGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            return DoctrineMetadataUtil.getObjectRepositoryLookupElements(getProject());
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String psiText = PsiElementUtils.getText(element);
            if(StringUtils.isBlank(psiText)) {
                return Collections.emptyList();
            }

            return new ArrayList<>(DoctrineMetadataUtil.getClassInsideScope(element, psiText));
        }
    }
}
