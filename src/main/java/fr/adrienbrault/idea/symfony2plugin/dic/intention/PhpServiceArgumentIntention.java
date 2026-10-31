package fr.adrienbrault.idea.symfony2plugin.dic.intention;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.PhpServiceArgumentIntention", fileExtensions = "php", categories = "Symfony")
public class PhpServiceArgumentIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(Project project, Editor editor, PsiElement psiElement) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return;
        }

        Set<String> serviceNames = ContainerCollectionResolver.ServiceCollector.create(project)
            .convertClassNameToServices(phpClass.getFQN());
        if (serviceNames.isEmpty()) {
            return;
        }

        List<PsiElement> psiElements = new ArrayList<>();
        for (String serviceName : serviceNames) {
            psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
        }

        if (psiElements.isEmpty()) {
            return;
        }

        Map<String, PsiElement> map = new HashMap<>();

        for (PsiElement element : psiElements) {
            map.put(VirtualFileUtil.getRelativePath(element.getContainingFile().getVirtualFile(), ProjectUtil.getProjectDir(element)), element);
        }

        JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<>(map.keySet()))
            .setTitle("Symfony: Services Definitions")
            .setItemChosenCallback(selectedValue ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Service Update")
                    .<RuntimeException>run(() -> invokeByScope(project, map.get(selectedValue), editor))
            )
            .createPopup();

        editor.showPopupInBestPositionFor(popup);
    }

    private void invokeByScope(Project project, PsiElement psiElement, Editor editor) {
        boolean success = false;

        if (psiElement instanceof XmlTag xmlTag) {
            List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(
                xmlTag,
                true,
                new ContainerCollectionResolver.LazyServiceCollector(xmlTag.getProject())
            );

            success = !args.isEmpty();
            if (success) {
                ServiceActionUtil.fixServiceArgument(args, xmlTag);
            }
        }
        else if (psiElement instanceof YAMLKeyValue yamlKeyValue) {
            List<String> args = ServiceActionUtil.getYamlMissingArgumentTypes(
                project,
                ServiceActionUtil.ServiceYamlContainer.create(yamlKeyValue),
                false,
                new ContainerCollectionResolver.LazyServiceCollector(yamlKeyValue.getProject())
            );

            success = !args.isEmpty();
            if (success) {
                ServiceActionUtil.fixServiceArgument(yamlKeyValue);
            }
        }

        if (!success) {
            IdeHelper.showErrorHintIfAvailable(editor, "No argument update needed");
            return;
        }

        String relativePath = VirtualFileUtil.getRelativePath(
            psiElement.getContainingFile().getVirtualFile(),
            ProjectUtil.getProjectDir(psiElement)
        );
        if (relativePath == null) {
            relativePath = "n/a";
        }

        HintManager.getInstance().showInformationHint(editor, String.format("Argument updated: %s", relativePath));
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement psiElement) {
        return PhpLanguage.INSTANCE.equals(psiElement.getLanguage()) && !getServicesInScope(project, psiElement).isEmpty();
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Symfony: Update service arguments");
    }

    private Set<String> getServicesInScope(Project project, PsiElement psiElement) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);

        return phpClass == null
            ? Collections.emptySet()
            : ContainerCollectionResolver.ServiceCollector.create(project).convertClassNameToServices(phpClass.getFQN());
    }
}
