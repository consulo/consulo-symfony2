package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilderEx;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigFoldingBuilder extends FoldingBuilderEx {

    @Override
    public FoldingDescriptor[] buildFoldRegions(PsiElement psiElement, Document document, boolean b) {
        if (!(psiElement instanceof TwigFile)) {
            return new FoldingDescriptor[0];
        }

        Project project = psiElement.getProject();
        Settings settings = Settings.getEnabledInstance(psiElement);
        if (settings == null) {
            return new FoldingDescriptor[0];
        }

        List<FoldingDescriptor> descriptors = new ArrayList<>();

        if (settings.codeFoldingTwigRoute) {
            attachPathFoldingDescriptors(project, psiElement, descriptors);
        }

        if (settings.codeFoldingTwigTemplate) {
            attachTemplateFoldingDescriptors(psiElement, descriptors);
        }

        if (settings.codeFoldingTwigConstant) {
            attachConstantFoldingDescriptors(psiElement, descriptors);
        }

        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void attachTemplateFoldingDescriptors(PsiElement psiElement, List<FoldingDescriptor> descriptors) {
        // find path calls in file
        ElementPattern<PsiElement> templateFileReferenceTagPattern = TwigPattern.getTemplateFileReferenceTagPattern();
        PsiElementPattern.Capture<PsiElement> formThemeFileTagPattern = TwigPattern.getFormThemeFileTagPattern();

        PsiElement[] fileReferences = PsiTreeUtil.collectElements(psiElement, psiElement1 ->
            templateFileReferenceTagPattern.accepts(psiElement1) || formThemeFileTagPattern.accepts(psiElement1)
        );

        for(PsiElement fileReference: fileReferences) {
            final String templateShortcutName = TwigUtil.getFoldingTemplateName(fileReference.getText());
            if(templateShortcutName != null) {
                descriptors.add(new FoldingDescriptor(fileReference.getNode(),
                    new TextRange(fileReference.getTextRange().getStartOffset(), fileReference.getTextRange().getEndOffset())) {
                    @Override
                    public String getPlaceholderText() {
                        return templateShortcutName;
                    }
                });
            }
        }

    }

    private void attachPathFoldingDescriptors(Project project, PsiElement psiElement, List<FoldingDescriptor> descriptors) {

        // find path calls in file
        ElementPattern<PsiElement> autocompletableRoutePattern = TwigPattern.getAutocompletableRoutePattern();
        PsiElement[] psiElements = PsiTreeUtil.collectElements(psiElement, autocompletableRoutePattern::accepts);

        if(psiElements.length == 0) {
            return;
        }

        Map<String,Route> routes = null;
        for(PsiElement psiElement1: psiElements) {

            // cache routes if we need them
            if(routes == null) {
                routes = RouteHelper.getAllRoutes(project);
            }

            String contents = PsiElementUtils.trimQuote(psiElement1.getText());
            if(!contents.isEmpty() && routes.containsKey(contents)) {
                final Route route = routes.get(contents);

                final String url = RouteHelper.getRouteUrl(route);
                if(url != null) {
                    descriptors.add(new FoldingDescriptor(psiElement1.getNode(),
                        new TextRange(psiElement1.getTextRange().getStartOffset(), psiElement1.getTextRange().getEndOffset())) {
                        @Override
                        public String getPlaceholderText() {
                            return url;
                        }
                    });
                }
            }

        }

    }

    private void attachConstantFoldingDescriptors(PsiElement psiElement, List<FoldingDescriptor> descriptors) {
        // find path calls in file
        ElementPattern<PsiElement> constant = TwigPattern.getPrintBlockOrTagFunctionPattern("constant");
        PsiElement[] constantReferences = PsiTreeUtil.collectElements(psiElement, constant::accepts);

        for (PsiElement fileReference: constantReferences) {
            String contents = fileReference.getText();
            if (StringUtils.isNotBlank(contents) && contents.contains(":")) {
                final String[] parts = contents.split("::");
                if (parts.length == 2) {
                    descriptors.add(new FoldingDescriptor(fileReference.getNode(),
                        new TextRange(fileReference.getTextRange().getStartOffset(), fileReference.getTextRange().getEndOffset())) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return parts[1];
                        }
                    });
                }
            }
        }
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
        return TwigLanguage.INSTANCE;
    }
}
