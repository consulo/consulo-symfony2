package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.xml.language.XMLLanguage;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.VirtualFilePattern;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlToken;
import com.jetbrains.twig.elements.TwigCompositeElement;
import consulo.annotation.component.ExtensionImpl;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
  * {% trans from "app" %}<caret>{% endtrans %}
 *  {% trans_default_domain "app" %}{% trans %}<caret>{% endtrans %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TranslationTagGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {% trans from "app" %}<caret>{% endtrans %}
        registrar.register(
            getTranslationTagValuePattern(), psiElement -> {
                TwigCompositeElement element = getTagOnTwigViewProvider(psiElement);
                if(element == null) {
                    return null;
                }

                String domain = TwigUtil.getDomainFromTranslationTag(element);
                if(domain == null) {
                    domain = TwigUtil.getTransDefaultDomainOnScope(element);
                }

                // overall fallback if no "trans*" scope was found
                if(domain == null) {
                    domain = "messages";
                }

                return new MyTranslationGotoCompletionProvider(element, domain);
            }
        );
    }

    private PsiElementPattern.Capture<XmlToken> getTranslationTagValuePattern() {
        return PlatformPatterns.psiElement(XmlToken.class)
            .inVirtualFile(new VirtualFilePattern().withExtension("twig"))
            .withLanguage(XMLLanguage.INSTANCE);
    }

    /**
     * Find trans tag as this is
     * <caret>{% endtrans %}
     */
    private static TwigCompositeElement getTagOnTwigViewProvider(PsiElement element) {
        PsiElement psiElement = TwigUtil.getElementOnTwigViewProvider(element);
        if(psiElement == null) {
            return null;
        }

        // end tag given, find start tag
        PsiElement prevSibling = psiElement.getPrevSibling();
        if(prevSibling instanceof TwigCompositeElement && prevSibling.getNode().getElementType() == TwigElementTypes.TAG) {
            return (TwigCompositeElement) prevSibling;
        }

        return null;
    }

    private static class MyTranslationGotoCompletionProvider extends GotoCompletionProvider {
        private final String domain;

        MyTranslationGotoCompletionProvider(PsiElement element, String domain) {
            super(element);
            this.domain = domain;
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            return TranslationUtil.getTranslationLookupElementsOnDomain(getProject(), domain);
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String text = element.getText();

            if(StringUtils.isBlank(text)) {
                return Collections.emptyList();
            }

            return Arrays.asList(
                TranslationUtil.getTranslationPsiElements(getProject(), text, domain)
            );
        }
    }
}
