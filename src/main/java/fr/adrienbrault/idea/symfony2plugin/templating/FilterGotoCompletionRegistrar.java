package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ContainerUtil;
import consulo.annotation.component.ExtensionImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtensionLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class FilterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {% trans foo<caret>bar %}
        registrar.register(TwigPattern.getFilterTagPattern(), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new MyFilterTagGotoCompletionProvider(psiElement);
        });
    }

    /**
     * {% trans foo<caret>bar %}
     */
    private static class MyFilterTagGotoCompletionProvider extends GotoCompletionProvider {
        MyFilterTagGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            Project project = getProject();
            for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFilters(project).entrySet()) {
                lookupElements.add(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));

                lookupElements.addAll(TwigTemplateCompletionContributor.getTypesFilters(project, entry.getKey(), entry.getValue()));
            }

            return lookupElements;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String text = element.getText();
            if(StringUtils.isBlank(text)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<>();

            for (Map.Entry<String, TwigExtension> extension : TwigExtensionParser.getFilters(getProject()).entrySet()) {
                if(!text.equals(extension.getKey())) {
                    continue;
                }

                ContainerUtil.addIfNotNull(
                    targets,
                    TwigExtensionParser.getExtensionTarget(getProject(), extension.getValue())
                );
            }

            return targets;
        }
    }
}