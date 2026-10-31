package fr.adrienbrault.idea.symfony2plugin.action;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.roots.PhpNamespaceCompositeProvider;
import consulo.dataContext.DataContext;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.StreamUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewFileActionUtil {

    public static @Nullable PsiDirectory getSelectedDirectoryFromAction(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        IdeView view = dataContext.getData(IdeView.KEY);
        if (view == null) {
            return null;
        }

        PsiDirectory[] directories = view.getDirectories();
        return directories.length == 0 ? null : directories[0];

    }

    public static boolean isInGivenDirectoryScope(AnActionEvent event, String... directoryName) {
        DataContext dataContext = event.getDataContext();
        IdeView view = dataContext.getData(IdeView.KEY);
        if (view == null) {
            return false;
        }

        PsiDirectory[] directories = view.getDirectories();
        if (directories.length == 0) {
            return false;
        }

        List<String> list = Arrays.asList(directoryName);

        if (list.contains(directories[0].getName())) {
            return true;
        }

        PsiDirectory parent = directories[0].getParent();
        if (parent != null && list.contains(parent.getName())) {
            return true;
        }

        return false;
    }

    public static String guessCommandTemplateType(Project project, String namespace) {
        // Check if InvokableCommand is available (Symfony 7.3+)
        if (PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Console\\Command\\InvokableCommand")) {
            String normalizedNamespace = "\\" + org.apache.commons.lang3.StringUtils.strip(namespace, "\\") + "\\";
            Collection<PhpClass> commandClasses = PhpIndexUtil.getPhpClassInsideNamespace(project, normalizedNamespace);

            boolean hasExecuteMethod = false;

            // Iterate over each class in the same namespace
            for (PhpClass phpClass : commandClasses) {
                if (phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty() && !PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
                    continue;
                }

                if (phpClass.findOwnMethodByName("execute") != null) {
                    hasExecuteMethod = true;
                    break;
                }
            }

            // if existing commands use execute, use invokable template
            if (!hasExecuteMethod) {
                return "command_invokable";
            }
        }

        if (PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Console\\Attribute\\AsCommand")) {
            return "command_attributes";
        }

        boolean isDefaultName = PhpElementsUtil.getClassesInterface(project, "\\Symfony\\Component\\Console\\Command\\Command")
            .stream()
            .anyMatch(phpClass -> phpClass.findFieldByName("defaultName", false) != null);

        if (isDefaultName) {
            return "command_property";
        }

        return "command_configure";
    }

    public static String guessControllerTemplateType(Project project) {
        if (PhpElementsUtil.hasClassOrInterface(project, "\\Symfony\\Component\\Routing\\Attribute\\Route")) {
            return "controller_attributes";
        }

        return "controller_annotations";
    }

    public static String getCommandPrefix(PsiDirectory psiDirectory) {
        List<String> strings = PhpNamespaceCompositeProvider.INSTANCE.suggestNamespaces(psiDirectory).stream().filter(s -> !s.isBlank()).toList();
        if (!strings.isEmpty()) {
            Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(psiDirectory.getProject());

            LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
            for (String namespace : strings) {
                namespace = "\\" + org.apache.commons.lang3.StringUtils.strip(namespace, "\\") + "\\";

                for (SymfonyCommand command : commands) {
                    if (command.getFqn().startsWith(namespace)) {
                        String name = command.getName();
                        int i = name.indexOf(":");
                        if (i > 0) {
                            String key = name.substring(0, i);
                            sortedMap.put(key, sortedMap.getOrDefault(key, 0));
                        }
                    }
                }
            }

            if (!sortedMap.isEmpty()) {
                List<Map.Entry<String, Integer>> list = new ArrayList<>(sortedMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .toList());

                Collections.reverse(list);

                return list.get(0).getKey();
            }
        }

        for (SymfonyBundle bundle : new SymfonyBundleUtil(psiDirectory.getProject()).getBundles()) {
            if (bundle.isInBundle(psiDirectory.getVirtualFile())) {
                String name = bundle.getName();
                if (name.endsWith("Bundle")) {
                    name = name.substring(0, name.length() - "Bundle".length());
                }

                String underscore = StringUtils.underscore(name);
                if (!underscore.isBlank()) {
                    return underscore;
                }
            }
        }

        return "app";
    }


    public static @Nullable String getFileTemplateContent(String filename) {
        try (InputStream inputStream = PhpBundleFileFactory.class.getResourceAsStream(filename)) {
            if (inputStream == null) {
                return null;
            }
            // replace on windows, just for secure reasons
            byte[] bytes = StreamUtil.loadFromStream(inputStream);
            return new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            return null;
        }
    }
}
