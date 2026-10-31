package fr.adrienbrault.idea.symfony2plugin.codeInsight.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class GotoHandler implements GotoDeclarationHandler {


    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> psiTargets = new ArrayList<>();
        for(GotoCompletionContributor contributor: GotoCompletionUtil.getContributors(psiElement)) {
            GotoCompletionProviderInterface formReferenceCompletionContributor = contributor.getProvider(psiElement);
            if(formReferenceCompletionContributor != null) {
                psiTargets.addAll(formReferenceCompletionContributor.getPsiTargets(psiElement));
            }
        }

        return psiTargets.toArray(new PsiElement[0]);
    }

    @Override
    public LocalizeValue getActionText(DataContext dataContext) {
        return LocalizeValue.empty();
    }
}
