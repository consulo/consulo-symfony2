package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import consulo.annotation.component.ExtensionImpl;

import consulo.ui.image.Image;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderAbstract;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TranslationDomainToolboxProvider extends PhpToolboxProviderAbstract {

    @Override
    public Collection<LookupElement> getLookupElements(PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return TranslationUtil.getTranslationDomainLookupElements(parameter.getProject());
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return new HashSet<>(TranslationUtil.getDomainPsiFiles(parameter.getProject(), parameter.getContents()));
    }

    @Override
    public String getName() {
        return "symfony.translation.domains";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Image getIcon() {
                return Symfony2Icons.TRANSLATION;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Symfony translation domains";
            }
        };
    }

}
