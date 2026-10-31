package fr.adrienbrault.idea.symfony2plugin.routing.documentation;

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
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.RouteDocumentationTargetProvider.RouteDocumentationContext;
import org.jspecify.annotations.Nullable;

/**
 * Documentation of a route name in PHP and Twig, like {@code generateUrl('app_blog')} or {@code {{ path('app_blog') }}}: the
 * routes behind the name, with a "Find usages" link
 */
@ExtensionImpl
public class RouteDocumentationProvider extends AbstractDocumentationProvider implements UnrestrictedDocumentationProvider, ExternalDocumentationHandler {
    @Override
    @RequiredReadAction
    public @Nullable PsiElement getCustomDocumentationElement(Editor editor, PsiFile file, @Nullable PsiElement contextElement) {
        return getCustomDocumentationElement(editor, file, contextElement, editor.getCaretModel().getOffset());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getCustomDocumentationElement(Editor editor, PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        RouteDocumentationContext context = new RouteDocumentationTargetProvider().resolveContext(file, targetOffset);
        return context == null ? null : context.getAnchor();
    }

    @Override
    @RequiredReadAction
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        RouteDocumentationContext context = resolveAnchorContext(element);
        return context == null ? null : new RouteDocumentationTarget(context).computeDocumentationHint();
    }

    @Override
    public boolean handleExternal(PsiElement element, PsiElement originalElement) {
        return false;
    }

    @Override
    public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
        if (!RouteDocumentationTargetKt.ROUTE_FIND_USAGES_LINK.equals(link)) {
            return false;
        }

        DocumentationFindUsagesLinkHandler.findUsages(() -> {
            RouteDocumentationContext routeContext = resolveAnchorContext(context);
            return routeContext == null ? null : new RouteDocumentationTarget(routeContext).findUsagesContext();
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
     * The route context of an element which is the anchor of a route name, the element {@link #getCustomDocumentationElement} gave
     */
    @RequiredReadAction
    private static @Nullable RouteDocumentationContext resolveAnchorContext(@Nullable PsiElement element) {
        if (element == null || !element.isValid()) {
            return null;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        RouteDocumentationContext context = new RouteDocumentationTargetProvider().resolveContext(file, element.getTextOffset());
        return context != null && context.getAnchor().equals(element) ? context : null;
    }
}
