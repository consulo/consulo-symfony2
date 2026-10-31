package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateGotoCompletionRegistrar extends GotoCompletionProvider {
    public TemplateGotoCompletionRegistrar(PsiElement element) {
        super(element);
    }

    @Override
    public Collection<LookupElement> getLookupElements() {
        return TwigUtil.getAllTemplateLookupElements(getElement().getProject());
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String templateName = GotoCompletionUtil.getTextValueForElement(element);
        if(templateName == null) {
            return Collections.emptyList();
        }

        return new HashSet<>(TwigUtil.getTemplatePsiElements(getElement().getProject(), templateName));
    }
}
