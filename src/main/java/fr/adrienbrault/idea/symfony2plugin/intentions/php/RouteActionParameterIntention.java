package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpModifier;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intention action to add parameters to route action methods (e.g., Request).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.RouteActionParameterIntention", fileExtensions = "php", categories = "Symfony")
public class RouteActionParameterIntention extends PsiElementBaseIntentionAction implements Iconable {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return;
        }

        List<String> availableFqns = RouteActionParameterIntentionKt.getAvailableRouteActionParameterFqns(method);
        if (availableFqns.isEmpty()) {
            return;
        }

        // Build display names: "ClassName (namespace)"
        List<String> displayNames = new ArrayList<>();
        Map<String, String> displayToFqn = new LinkedHashMap<>();
        for (String fqn : availableFqns) {
            String displayName = RouteActionParameterIntentionKt.formatRouteActionParameterDisplayName(fqn);
            displayNames.add(displayName);
            displayToFqn.put(displayName, fqn);
        }

        editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(displayNames)
            .setTitle("Symfony: Add Parameter to Route Action")
            .setItemChosenCallback(selectedDisplay ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Add Route Action Parameter")
                    .<RuntimeException>run(() -> {
                        String fqn = displayToFqn.get(selectedDisplay);
                        String variableName = fqn != null ? RouteActionParameterIntentionKt.AVAILABLE_ROUTE_ACTION_PARAMETERS.get(fqn) : null;
                        if (variableName != null && fqn != null) {
                            PhpElementsUtil.addParameterToMethod(method, "\\" + fqn, variableName);
                        }
                    })
            )
            .createPopup());
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

        if (method.getAccess() != PhpModifier.Access.PUBLIC) {
            return false;
        }

        if (!RouteActionParameterIntentionKt.isMethodARouteAction(method)) {
            return false;
        }

        return !RouteActionParameterIntentionKt.getAvailableRouteActionParameterFqns(method).isEmpty();
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add parameter to route action");
    }

    @Override
    public Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }
}
