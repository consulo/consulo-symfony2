package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.documentation.AbstractDocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationHandler;
import consulo.language.editor.documentation.UnrestrictedDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.documentation.DocumentationFindUsagesLinkHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Documentation of a Twig template name in PHP and Twig, like {@code render('blog/index.html.twig')} or
 * {@code {% include 'blog/_footer.html.twig' %}}: the template files, their usages and variables, with a "Find usages" link
 */
@ExtensionImpl
public class TwigTemplateDocumentationProvider extends AbstractDocumentationProvider implements UnrestrictedDocumentationProvider, ExternalDocumentationHandler {
    @Override
    @RequiredReadAction
    public @Nullable PsiElement getCustomDocumentationElement(Editor editor, PsiFile file, @Nullable PsiElement contextElement) {
        return getCustomDocumentationElement(editor, file, contextElement, editor.getCaretModel().getOffset());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getCustomDocumentationElement(Editor editor, PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        TwigTemplateDocumentationContext context = new TwigTemplateDocumentationTargetProvider().resolveContext(file, targetOffset);
        return context == null ? null : context.getAnchor();
    }

    @Override
    @RequiredReadAction
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        TwigTemplateDocumentationContext context = resolveAnchorContext(element);
        if (context == null) {
            return null;
        }
        return TwigTemplateDocumentationTargetProviderKt.renderTwigTemplateDocumentation(
            TwigTemplateDocumentationTargetKt.collectTwigTemplateDocumentation(context)
        );
    }

    @Override
    public boolean handleExternal(PsiElement element, PsiElement originalElement) {
        return false;
    }

    @Override
    public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
        if (!TwigTemplateDocumentationTargetKt.TEMPLATE_FIND_USAGES_LINK.equals(link)) {
            return false;
        }

        DocumentationFindUsagesLinkHandler.findUsages(() -> {
            if (resolveAnchorContext(context) == null) {
                return null;
            }
            // the first template file, in path order, like the first documentation target
            List<TwigTemplateDocumentationTarget> targets =
                new TwigTemplateDocumentationTargetProvider().documentationTargets(context.getContainingFile(), context.getTextOffset());
            return targets.isEmpty() ? null : targets.get(0).findUsagesContext();
        });
        return true;
    }

    @Override
    public boolean canFetchDocumentationLink(String link) {
        return false;
    }

    @Override
    public @Nullable String fetchExternalDocumentation(String link, @Nullable PsiElement element) {
        return null;
    }

    @Override
    public boolean canHandleExternal(@Nullable PsiElement element, @Nullable PsiElement originalElement) {
        return false;
    }

    /**
     * The template context of an element which is the anchor of a template name, the element {@link #getCustomDocumentationElement}
     * gave
     */
    @RequiredReadAction
    private static @Nullable TwigTemplateDocumentationContext resolveAnchorContext(@Nullable PsiElement element) {
        if (element == null || !element.isValid()) {
            return null;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        TwigTemplateDocumentationContext context = new TwigTemplateDocumentationTargetProvider().resolveContext(file, element.getTextOffset());
        return context != null && context.getAnchor().equals(element) ? context : null;
    }
}
