package fr.adrienbrault.idea.symfony2plugin.action;

import com.jetbrains.php.refactoring.PhpNameUtil;
import com.jetbrains.php.roots.PhpNamespaceCompositeProvider;
import consulo.application.Application;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.localize.LocalizeValue;
import consulo.ui.InputBoxBuilder;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractNewPhpClassAction extends AbstractProjectDumbAwareAction {

    public AbstractNewPhpClassAction(String text, String description) {
        super(text, description, Symfony2Icons.SYMFONY);
    }

    /**
     * Returns a suffix to auto-append to the class name (e.g. "Command", "Test").
     * The suffix is appended only when the class name does not already end with it (case-insensitive).
     * Return null to skip suffix handling.
     */
    protected @Nullable String getClassNameSuffix() {
        return null;
    }

    /**
     * Returns the file template name to use for code generation.
     *
     * @param project   the current project
     * @param namespace the resolved namespace for the target directory
     * @param directory the target directory
     */
    protected abstract String getTemplateName(Project project, String namespace, PsiDirectory directory);

    /**
     * Returns additional template variables beyond the default "class" and "namespace" entries.
     *
     * @param className the final (possibly suffix-appended) class name
     * @param namespace the resolved namespace
     * @param directory the target directory
     */
    protected Map<String, String> getExtraVariables(String className, String namespace, PsiDirectory directory) {
        return Collections.emptyMap();
    }

    @Override
    public void update(AnActionEvent event) {
        this.setStatus(event, false);
        Project project = event.getData(Project.KEY);
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        if (NewFileActionUtil.getSelectedDirectoryFromAction(event) != null) {
            this.setStatus(event, true);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        PsiDirectory directory = NewFileActionUtil.getSelectedDirectoryFromAction(event);
        if (directory == null) {
            return;
        }

        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(LocalizeValue.localizeTODO("New File"))
            .text(LocalizeValue.localizeTODO("New class name:"))
            .icon(Symfony2Icons.SYMFONY)
            .showAsync(project)
            .whenCompleteAsync((className, error) -> {
                if (error == null && StringUtils.isNotBlank(className)) {
                    createClass(project, directory, className);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void createClass(Project project, PsiDirectory directory, String inputClassName) {
        String className = inputClassName;

        String suffix = getClassNameSuffix();
        if (suffix != null && !className.toLowerCase().endsWith(suffix.toLowerCase())) {
            className += suffix;
        }

        if (!PhpNameUtil.isValidClassName(className)) {
            MessageBoxes.okInfo(LocalizeValue.localizeTODO("Invalid class name")).showAsync(project);
            return;
        }

        List<String> namespaces = PhpNamespaceCompositeProvider.INSTANCE.suggestNamespaces(directory);
        if (namespaces.isEmpty()) {
            MessageBoxes.okInfo(LocalizeValue.localizeTODO("No namespace found")).showAsync(project);
            return;
        }

        String finalClassName = className;
        String namespace = namespaces.get(0);
        String templateName = getTemplateName(project, namespace, directory);
        Map<String, String> extraVariables = getExtraVariables(finalClassName, namespace, directory);

        Application.get().runWriteAction(() -> {
            HashMap<String, String> vars = new HashMap<>();
            vars.put("class", finalClassName);
            vars.put("namespace", namespace);
            vars.putAll(extraVariables);

            PsiElement element = PhpBundleFileFactory.createFile(
                project,
                directory.getVirtualFile(),
                templateName,
                finalClassName,
                vars
            );

            OpenFileDescriptorFactory.getInstance(project).newBuilder(element.getContainingFile().getVirtualFile()).offset(0).build().navigate(true);
        });
    }
}
