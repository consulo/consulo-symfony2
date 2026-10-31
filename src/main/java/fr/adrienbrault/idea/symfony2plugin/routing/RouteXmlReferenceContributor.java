package fr.adrienbrault.idea.symfony2plugin.routing;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.psi.ResolveResult;
import consulo.language.util.ProcessingContext;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlText;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteXmlReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.and(XmlHelper.XML_EXTENSION, XmlHelper.getRouteControllerPattern()),
            new PsiReferenceProvider() {
                @Override
                public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new RouteActionReference(psiElement)
                    };
                }

                @Override
                public boolean acceptsTarget(PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    private static class RouteActionReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {
        private RouteActionReference(PsiElement psiElement) {
            super(psiElement);
        }

        @Override
        public ResolveResult[] multiResolve(boolean b) {
            PsiElement parent = getElement().getParent();
            if (parent == null) {
                return new ResolveResult[0];
            }

            String text = getControllerText(parent);
            if(text == null) {
                return new ResolveResult[0];
            }

            return PsiElementResolveResult.createResults(
                RouteHelper.getMethodsOnControllerShortcut(getElement().getProject(), text)
            );
        }

        @Override
        public Object[] getCompletionVariants() {
            return ControllerIndex.getControllerLookupElements(getElement().getProject()).toArray();
        }
    }

    /**
     * <default key="_controller">Fo<caret>o\Bar</default>
     * <route controller=""/>
     */
    @Nullable
    public static String getControllerText(PsiElement parent) {
        String text = null;
        if(parent instanceof XmlText) {
            // <route><default key="_controller">Fo<caret>o\Bar</default></route>
            text = parent.getText();
        } else if(parent instanceof XmlAttribute) {
            // <route controller=""/>
            text = ((XmlAttribute) parent).getValue();
        }

        if (StringUtils.isBlank(text)) {
            return null;
        }

        return text;
    }
}
