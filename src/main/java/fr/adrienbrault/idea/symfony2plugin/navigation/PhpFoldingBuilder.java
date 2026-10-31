package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilderEx;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpFoldingBuilder extends FoldingBuilderEx {

    @Override
    public FoldingDescriptor[] buildFoldRegions(PsiElement psiElement, Document document, boolean b) {
        if (!(psiElement instanceof PhpFile)){
            return new FoldingDescriptor[0];
        }

        Project project = psiElement.getProject();
        Settings settings = Settings.getEnabledInstance(psiElement);
        if (settings == null){
            return new FoldingDescriptor[0];
        }

        boolean codeFoldingPhpRoute = settings.codeFoldingPhpRoute;
        boolean codeFoldingPhpModel = settings.codeFoldingPhpModel;
        boolean codeFoldingPhpTemplate = settings.codeFoldingPhpTemplate;

        // we dont need to do anything
        if(!codeFoldingPhpRoute && !codeFoldingPhpModel && !codeFoldingPhpTemplate) {
            return new FoldingDescriptor[0];
        }

        List<FoldingDescriptor> descriptors = new ArrayList<>();

        Collection<StringLiteralExpression> stringLiteralExpressiones = PsiTreeUtil.findChildrenOfType(psiElement, StringLiteralExpression.class);
        for(StringLiteralExpression stringLiteralExpression: stringLiteralExpressiones) {

            if(codeFoldingPhpModel) {
                attachModelShortcuts(descriptors, stringLiteralExpression);
            }

            if(codeFoldingPhpTemplate) {
                attachTemplateShortcuts(descriptors, stringLiteralExpression);
            }

        }

        // strip ".[php|html].twig"
        if(codeFoldingPhpRoute) {
            attachRouteShortcuts(project, descriptors, stringLiteralExpressiones);
        }

        return descriptors.toArray(new FoldingDescriptor[0]);
    }


    private void attachRouteShortcuts(Project project, List<FoldingDescriptor> descriptors, Collection<StringLiteralExpression> stringLiteralExpressions) {

        Map<String,Route> routes = null;

        for(StringLiteralExpression stringLiteralExpression: stringLiteralExpressions) {

            if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, PhpRouteReferenceContributor.GENERATOR_SIGNATURES) != null) {

                // cache routes if we need them
                if(routes == null) {
                    routes = RouteHelper.getAllRoutes(project);
                }

                String contents = stringLiteralExpression.getContents();
                if(!contents.isEmpty() && routes.containsKey(contents)) {
                    final Route route = routes.get(contents);

                    final String url = RouteHelper.getRouteUrl(route);
                    if(url != null) {
                        descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
                            new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
                            @Override
                            public String getPlaceholderText() {
                                return url;
                            }
                        });
                    }
                }
            }

        }

    }

    private void attachModelShortcuts(List<FoldingDescriptor> descriptors, final StringLiteralExpression stringLiteralExpression) {

        if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES) == null) {
            return;
        }

        String content = stringLiteralExpression.getContents();

        for(String lastChar: new String[] {":", "\\"}) {
            if(content.contains(lastChar)) {
                final String replace = content.substring(content.lastIndexOf(lastChar) + 1);
                if(!replace.isEmpty()) {
                    descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
                        new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return replace;
                        }
                    });
                }

                return;
            }
        }

    }

    private void attachTemplateShortcuts(List<FoldingDescriptor> descriptors, final StringLiteralExpression stringLiteralExpression) {

        if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES) == null) {
            return;
        }

        String content = stringLiteralExpression.getContents();

        String templateShortcutName = TwigUtil.getFoldingTemplateName(content);
        if(templateShortcutName == null) {
            return;
        }

        final String finalTemplateShortcutName = templateShortcutName;
        descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
            new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
            @Nullable
            @Override
            public String getPlaceholderText() {
                return finalTemplateShortcutName;
            }
        });

    }

    @Nullable
    @Override
    public String getPlaceholderText(ASTNode astNode) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(ASTNode astNode) {
        return true;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
