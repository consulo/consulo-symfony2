package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
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
 * Intention action to add parameters to the __invoke method of an invokable Symfony Command.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.CommandInvokeParameterIntention", fileExtensions = "php", categories = "Symfony")
public class CommandInvokeParameterIntention extends PsiElementBaseIntentionAction implements Iconable {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return;
        }

        List<String> availableFqns = CommandInvokeParameterIntentionKt.getAvailableCommandParameterFqns(invokeMethod);
        if (availableFqns.isEmpty()) {
            return;
        }

        // Build display names: "ClassName (namespace)"
        List<String> displayNames = new ArrayList<>();
        Map<String, String> displayToFqn = new LinkedHashMap<>();
        for (String fqn : availableFqns) {
            String displayName = CommandInvokeParameterIntentionKt.formatCommandParameterDisplayName(fqn);
            displayNames.add(displayName);
            displayToFqn.put(displayName, fqn);
        }

        editor.showPopupInBestPositionFor(JBPopupFactory.getInstance().createPopupChooserBuilder(displayNames)
            .setTitle("Symfony: Add Parameter to __invoke")
            .setItemChosenCallback(selectedDisplay ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Add __invoke Parameter")
                    .<RuntimeException>run(() -> {
                        String fqn = displayToFqn.get(selectedDisplay);
                        String variableName = fqn != null ? CommandInvokeParameterIntentionKt.AVAILABLE_COMMAND_PARAMETERS.get(fqn) : null;
                        if (variableName != null && fqn != null) {
                            PhpElementsUtil.addParameterToMethod(invokeMethod, "\\" + fqn, variableName);
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

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return false;
        }

        if (phpClass.getAttributes(CommandInvokeParameterIntentionKt.AS_COMMAND_ATTRIBUTE).isEmpty()) {
            return false;
        }

        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return false;
        }

        return !CommandInvokeParameterIntentionKt.getAvailableCommandParameterFqns(invokeMethod).isEmpty();
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Add parameter to __invoke");
    }

    @Override
    public Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }
}
