package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.document.Document;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeScopeValidator;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;

import java.util.Collection;

/**
 * Intention action to add #[\Symfony\Component\Routing\Attribute\Route] attribute to a public method in a controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.AddRouteAttributeIntention", fileExtensions = "php", categories = "Symfony")
public class AddRouteAttributeIntention extends PsiElementBaseIntentionAction implements Iconable {
    private static final String ROUTE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\Routing\\Attribute\\Route";

    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return;
        }

        String routeName = AnnotationBackportUtil.getRouteByMethod(method);
        if (routeName == null) {
            routeName = "";
        }

        String routePath = AnnotationBackportUtil.getRoutePathByMethod(method);
        if (routePath == null) {
            routePath = "/";
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(method.getContainingFile());
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

        String importedRouteName = PhpElementsUtil.insertUseIfNecessary(phpClass, ROUTE_ATTRIBUTE_CLASS);
        if (importedRouteName == null) {
            importedRouteName = "Route";
        }

        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = method.getTextRange().getStartOffset();
        String attributePrefix = "#[" + importedRouteName + "('";
        String attributeText = attributePrefix + routePath + "', name: '" + routeName + "')]\n";

        document.insertString(methodStartOffset, attributeText);

        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);

        // position caret after the opening quote of the path: #[Route('<caret>/...
        if (editor != null) {
            int caretOffset = methodStartOffset + attributePrefix.length();
            editor.getCaretModel().moveToOffset(caretOffset);
        }
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        // consulo-port: plugin.xml <language>PHP</language>
        if (!psiElement.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return false;
        }

        if (!method.getAccess().isPublic() || method.isStatic()) {
            return false;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return false;
        }

        if (hasRouteAttribute(method)) {
            return false;
        }

        if (!PhpAttributeScopeValidator.isControllerClass(phpClass)) {
            return false;
        }

        return PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_CLASS);
    }

    private boolean hasRouteAttribute(Method method) {
        for (String routeAnnotation : RouteHelper.ROUTE_ANNOTATIONS) {
            Collection<PhpAttribute> attributes = method.getAttributes(routeAnnotation);
            if (!attributes.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add Route attribute");
    }

    @Override
    public Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }
}
