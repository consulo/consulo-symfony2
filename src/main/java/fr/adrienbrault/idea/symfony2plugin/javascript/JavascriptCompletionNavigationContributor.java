package fr.adrienbrault.idea.symfony2plugin.javascript;

import com.intellij.lang.javascript.psi.JSLiteralExpression;
import consulo.language.pattern.InitialPatternCondition;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.lang.javascript.psi.JSParameter;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.patterns.JSPatterns;
import com.intellij.lang.javascript.patterns.JSElementPattern;
import consulo.javascript.lang.JavaScriptTokenSets;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JavascriptCompletionNavigationContributor {

    static final InsertHandler<LookupElement> INSERT_HANDLER_ROUTE_PATH_REPLACE = new MyLookupElementReplaceRouteWithPath();

    /**
     * consulo-port: registered through {@link JavascriptCompletionNavigationContributorGotoDeclaration}
     */
    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (!Symfony2ProjectComponent.isEnabled(psiElement) || !JavaScriptTokenSets.STRING_LITERALS.contains(psiElement.getNode().getElementType()) || !(psiElement.getContext() instanceof JSLiteralExpression jsLiteralExpression)) {
                return null;
            }

            if (isAcceptedRouteNamePattern(jsLiteralExpression)) {
                String routeName = jsLiteralExpression.getStringValue();
                if (routeName == null || routeName.isBlank()) {
                    return new PsiElement[0];
                }

                return RouteHelper.getRouteDefinitionTargets(psiElement.getProject(), routeName).toArray(new PsiElement[0]);
            }

            if (!isAcceptedUrlPattern(jsLiteralExpression)) {
                return null;
            }

            String content = jsLiteralExpression.getStringValue();
            if (content != null && !content.isBlank()) {
                return RouteHelper.getMethodsForPathWithPlaceholderMatch(psiElement.getProject(), content);
            }

            return new PsiElement[0];
        }
    }

    /**
     * insertHandler are after already inserted string, so remove it and replace it :)
     */
    private static class MyLookupElementReplaceRouteWithPath implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(InsertionContext context, LookupElement item) {
            Document document = context.getDocument();

            document.deleteString(context.getStartOffset(), context.getTailOffset());

            Route route = (Route) item.getObject();
            document.insertString(context.getTailOffset(), route.getPath());
            context.commitDocument();

            context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
        }
    }

    /**
     * FOSJsRoutingBundle exposes the routing as a route name, not as an url
     *
     * Routing.generate('foo_route')
     * fos.Router.generate('foo_route')
     */
    static boolean isAcceptedRouteNamePattern(final JSLiteralExpression jsLiteral) {
        return JSPatterns.jsLiteralExpression()
            .inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("Routing.generate"), 0))
            .accepts(jsLiteral)

            || JSPatterns.jsLiteralExpression()
            .inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("fos.Router.generate"), 0))
            .accepts(jsLiteral);
    }

    static boolean isAcceptedUrlPattern(final JSLiteralExpression jsLiteral) {
        return
            // axios.create('')
            // axios.create(baseURL: '')
            JSPatterns.jsLiteralExpression()
                .inside(false, JSPatterns.jsProperty().withName("baseURL").withParent(JSObjectLiteralExpression.class))
                .inside(false, JSPatterns.or(
                    JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("axios.create"), 0)
                ))
                .accepts(jsLiteral)

                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("axios"), 0)).accepts(jsLiteral)
                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("fetch"), 0)).accepts(jsLiteral)

                // new Request( '')
                || JSPatterns.jsLiteralExpression(JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("Request"), 0)).accepts(jsLiteral)

                // {url: ''}
                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsExpression().withParent(JSPatterns.jsProperty().withName("url"))).accepts(jsLiteral)

                // allows basically to support all common use cases by resolve the argument "named argument"
                // foobar('foobar') => foobar(url)
                || JSPatterns.jsLiteralExpression().inside(false, jsArgumentOfName("url")).accepts(jsLiteral)
            ;
    }

    private static JSElementPattern.Capture<JSExpression> jsArgumentOfName(final String name) {
        return new JSElementPattern.Capture<>(new InitialPatternCondition<>(JSExpression.class) {
            @Override
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                if (!(o instanceof JSExpression expression) || !(expression.getParent() instanceof JSArgumentList argumentList)) {
                    return false;
                }
                JSParameter parameter = JSResolveUtil.findParameterForUsedArgument(expression, argumentList);
                return parameter != null && Objects.equals(parameter.getName(), name);
            }
        });
    }
}
