package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TaggedParameterGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // arguments: [!tagged twig.extension]
        // <argument type="tagged" tag="foobar" />
        registrar.register(
            PlatformPatterns.or(
                YamlElementPatternHelper.getTaggedServicePattern(),
                YamlElementPatternHelper.getSingleLineScalarKey("tag"),
                YamlElementPatternHelper.getTaggedIteratorTagNamePattern(),
                XmlPatterns.psiElement().withParent(XmlHelper.getTypeTaggedTagAttribute())
            ),
            new MyTagGotoCompletionContributor()
        );
    }

    private static class MyTagGotoCompletionContributor implements GotoCompletionContributor {
        @Override
        public GotoCompletionProvider getProvider(PsiElement psiElement) {
            return new GotoCompletionProvider(psiElement) {
                @Override
                public Collection<LookupElement> getLookupElements() {
                    return TagNameCompletionProvider.getTagLookupElements(getProject());
                }

                @Override
                public Collection<PsiElement> getPsiTargets(PsiElement element) {
                    String tagName = GotoCompletionUtil.getTextValueForElement(element);
                    if(tagName == null) {
                        return Collections.emptyList();
                    }

                    return new ArrayList<>(ServiceUtil.getTaggedClasses(element.getProject(), tagName));
                }
            };
        }
    }
}
