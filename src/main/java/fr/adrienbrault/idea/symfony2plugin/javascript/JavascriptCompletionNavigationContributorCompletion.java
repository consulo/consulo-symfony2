package fr.adrienbrault.idea.symfony2plugin.javascript;

import consulo.javascript.lang.JavaScriptTokenSets;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.javascript.language.JavaScriptLanguage;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;

/**
 * consulo-port: moved from the nested class {@code JavascriptCompletionNavigationContributor.Completion}
 * (nested classes can not be registered as extensions)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class JavascriptCompletionNavigationContributorCompletion extends CompletionContributor {
    public JavascriptCompletionNavigationContributorCompletion() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(JavaScriptTokenSets.STRING_LITERALS).withParent(JSLiteralExpression.class), new CompletionParametersCompletionProvider());
    }

    @Override
    public Language getLanguage() {
        return JavaScriptLanguage.INSTANCE;
    }

    private static class CompletionParametersCompletionProvider implements CompletionProvider {
        @Override
        public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
            PsiElement psiElement = parameters.getOriginalPosition();
            if (!Symfony2ProjectComponent.isEnabled(psiElement) || !JavaScriptTokenSets.STRING_LITERALS.contains(psiElement.getNode().getElementType()) || !(psiElement.getContext() instanceof JSLiteralExpression jsLiteral)) {
                return;
            }

            if (JavascriptCompletionNavigationContributor.isAcceptedRouteNamePattern(jsLiteral)) {
                result.addAllElements(RouteHelper.getRoutesLookupElements(psiElement.getProject()));

                return;
            }

            if (!JavascriptCompletionNavigationContributor.isAcceptedUrlPattern(jsLiteral)) {
                return;
            }

            // add by resolved path
            result.addAllElements(RouteHelper.getRoutesPathLookupElements(psiElement.getProject()));

            // provide route name, but replace it by path on insert
            for (Route route : RouteHelper.getAllRoutes(psiElement.getProject()).values()) {
                String path = route.getPath();
                if (path != null) {
                    LookupElementBuilder lookupElementBuilder = LookupElementBuilder
                        .create(route, route.getName())
                        .withIcon(Symfony2Icons.SYMFONY)
                        .withTypeText(path)
                        .withInsertHandler(JavascriptCompletionNavigationContributor.INSERT_HANDLER_ROUTE_PATH_REPLACE);

                    result.addElement(lookupElementBuilder);
                }
            }
        }
    }
}
