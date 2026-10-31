package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.process.local.EnvironmentUtil;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class SymfonyCommandTestRunLineMarkerProvider extends RunLineMarkerContributor {
    @Override
    @RequiredReadAction
    public @Nullable Info getInfo(PsiElement leaf) {
        PhpNamedElement commandTarget = getCommandTargetContext(leaf);
        if (commandTarget == null) {
            return null;
        }

        List<String> commandNames = getCommandNames(commandTarget);
        if (commandNames.isEmpty()) {
            return null;
        }

        String commandName = commandNames.getFirst();
        List<AnAction> actions = new ArrayList<>();

        Collections.addAll(actions, ExecutorAction.getActions());

        Project project = leaf.getProject();
        VirtualFile projectDir = ProjectUtil.getProjectDir(project);

        if (projectDir.findFileByRelativePath("bin/console") != null) {
            actions.add(new SymfonyCommandRunAction(
                SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER,
                "Run '" + commandName + "' with bin/console",
                ExecutionIconGroup.gutterRun()
            ));
        }

        if (isSymfonyCliAvailable()) {
            actions.add(new SymfonyCommandRunAction(
                SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI,
                "Run '" + commandName + "' with Symfony CLI",
                ExecutionIconGroup.gutterRun()
            ));
        }

        return new Info(
            ExecutionIconGroup.gutterRun(),
            psiElement -> "Run Symfony Command",
            actions.toArray(AnAction.EMPTY_ARRAY)
        );
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    @Nullable
    public static PhpNamedElement getCommandTargetContext(PsiElement leaf) {
        if (PhpPsiUtil.isOfType(leaf, PhpTokenTypes.IDENTIFIER)) {
            PhpNamedElement element = ObjectUtil.tryCast(leaf.getParent(), PhpNamedElement.class);
            if (element != null && element.getNameIdentifier() == leaf) {
                if (element instanceof Method method && !SymfonyCommandUtil.getCommandNameFromMethod(method).isEmpty()) {
                    return method;
                }

                if (element instanceof PhpClass phpClass && !SymfonyCommandUtil.getCommandNameFromClass(phpClass).isEmpty()) {
                    return phpClass;
                }
            }
        }

        return null;
    }

    public static List<String> getCommandNames(PhpNamedElement target) {
        if (target instanceof Method method) {
            return SymfonyCommandUtil.getCommandNameFromMethod(method);
        }

        if (target instanceof PhpClass phpClass) {
            return SymfonyCommandUtil.getCommandNameFromClass(phpClass);
        }

        return List.of();
    }

    public static boolean isSymfonyCliAvailable() {
        String path = EnvironmentUtil.getValue("PATH");
        if (path == null) {
            return false;
        }

        for (String dir : path.split(File.pathSeparator)) {
            File file = new File(dir, "symfony");
            if (file.isFile() && file.canExecute()) {
                return true;
            }
        }

        return false;
    }
}
