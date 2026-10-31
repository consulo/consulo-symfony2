package fr.adrienbrault.idea.symfony2plugin.routing;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> lineMarkerInfos) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement : psiElements) {
            attachRouteActions(lineMarkerInfos, psiElement);
            attachUxComponents(lineMarkerInfos, psiElement);

            if(psiElement instanceof PhpFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }
    }

    private void attachUxComponents(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement leaf) {
        if (leaf.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return;
        }

        if (leaf.getParent() instanceof PhpClass phpClass) {
            Collection<PsiFile> files = new HashSet<>();
            for (String template : UxUtil.getComponentTemplatesForPhpClass(phpClass)) {
                files.addAll(TwigUtil.getTemplatePsiElements(phpClass.getProject(), template));
            }

            if (!files.isEmpty()) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_LINE_MARKER)
                    .setTargets(files)
                    .setTooltipText("Navigate to UX Component template");

                // leaf elements are only allowed to attach
                ASTNode nameNode = phpClass.getNameNode();
                if (nameNode != null) {
                    lineMarkerInfos.add(builder.createLineMarkerInfo(nameNode.getPsi()));
                }
            }
        }
    }

    private void attachRouteActions(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement leaf) {
        if (leaf.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return;
        }

        PsiElement psiElement = leaf.getParent();
        if (!(psiElement instanceof MethodReference methodCall) || !"controller".equalsIgnoreCase(((MethodReference) psiElement).getName()) || !PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) psiElement, "\\Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait", "controller")) {
            return;
        }

        PsiElement[] methods = RouteHelper.getPhpController(methodCall);
        if(methods.length > 0) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                    setTargets(methods).
                    setTooltipText("Navigate to action");

            // leaf elements are only allowed to attach; search "controller" psi element
            ASTNode nameNode = ((MethodReference) psiElement).getNameNode();
            if (nameNode != null) {
                lineMarkerInfos.add(builder.createLineMarkerInfo(nameNode.getPsi()));
            }
        }
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
