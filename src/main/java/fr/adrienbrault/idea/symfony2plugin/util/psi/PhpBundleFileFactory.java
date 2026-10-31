package fr.adrienbrault.idea.symfony2plugin.util.psi;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.editor.WriteCommandAction;
import consulo.localize.LocalizeValue;
import consulo.ui.InputBoxBuilder;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.psi.PsiManager;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import com.jetbrains.php.refactoring.PhpNameUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.NewFileActionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpBundleFileFactory {

    /**
     * Consulo: the class name is asked asynchronously, the compiler pass is created in its own write command
     */
    @RequiredUIAccess
    public static void invokeCreateCompilerPass(PhpClass bundleClass, @Nullable Editor editor) {
        Project project = bundleClass.getProject();

        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(LocalizeValue.localizeTODO("New File"))
            .text(LocalizeValue.localizeTODO("Class name for CompilerPass (no namespace needed): "))
            .icon(Symfony2Icons.SYMFONY)
            .showAsync(project)
            .whenCompleteAsync((className, error) -> {
                if (error != null || StringUtils.isBlank(className)) {
                    return;
                }

                if (!PhpNameUtil.isValidClassName(className)) {
                    MessageBoxes.okError(LocalizeValue.localizeTODO("Invalid class name"))
                        .title(LocalizeValue.localizeTODO("Error"))
                        .showAsync(project);
                }

                Exception[] exception = new Exception[1];
                WriteCommandAction.runWriteCommandAction(project, "Create CompilerPass", null, () -> {
                    try {
                        PhpBundleFileFactory.createCompilerPass(bundleClass, className);
                    }
                    catch (Exception e) {
                        exception[0] = e;
                    }
                });

                if (exception[0] != null) {
                    String message = "Error:" + exception[0].getMessage();
                    if (editor != null) {
                        IdeHelper.showErrorHintIfAvailable(editor, message);
                    }
                    else {
                        MessageBoxes.okError(LocalizeValue.localizeTODO(message)).showAsync(project);
                    }
                }
            }, uiAccess);
    }

    public static PsiElement createBundleFile(PhpClass bundleClass, String template, String className, Map<String, String> vars) throws Exception {

        VirtualFile directory = bundleClass.getContainingFile().getContainingDirectory().getVirtualFile();
        if(fileExists(directory, new String[] {className})) {
            throw new Exception("File already exists");
        }

        String COMPILER_TEMPLATE = "/fileTemplates/" + template + ".php";
        String fileTemplateContent = NewFileActionUtil.getFileTemplateContent(COMPILER_TEMPLATE);
        if(fileTemplateContent == null) {
            throw new Exception("Template content error");
        }

        String[] split = className.split("\\\\");

        String ns = bundleClass.getNamespaceName();
        String join = StringUtils.join(Arrays.copyOf(split, split.length - 1), "/");

        vars.put("ns", (ns.startsWith("\\") ? ns.substring(1) : ns) + join.replace("/", "\\"));
        vars.put("class", split[split.length - 1]);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            fileTemplateContent = fileTemplateContent.replace("{{ " + entry.getKey() + " }}", entry.getValue());
        }

        VirtualFile compilerDirectory = getAndCreateDirectory(directory, join);
        if(compilerDirectory == null) {
            throw new Exception("Directory creation failed");
        }

        Project project = bundleClass.getProject();
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(split[split.length - 1] + ".php", PhpFileType.INSTANCE, fileTemplateContent);
        CodeStyleManager.getInstance(project).reformat(fileFromText);
        return PsiManager.getInstance(project).findDirectory(compilerDirectory).add(fileFromText);
    }

    public static PsiElement createFile(Project project, VirtualFile parentDirectory, String template, String filename, Map<String, String> vars) {
        String COMPILER_TEMPLATE = "/fileTemplates/" + template + ".php";
        String fileTemplateContent = NewFileActionUtil.getFileTemplateContent(COMPILER_TEMPLATE);
        if(fileTemplateContent == null) {
            throw new RuntimeException("Template content error");
        }

        for (Map.Entry<String, String> entry : vars.entrySet()) {
            fileTemplateContent = fileTemplateContent.replace("{{ " + entry.getKey() + " }}", entry.getValue());
        }

        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(filename + ".php", PhpFileType.INSTANCE, fileTemplateContent);
        CodeStyleManager.getInstance(project).reformat(fileFromText);
        return PsiManager.getInstance(project).findDirectory(parentDirectory).add(fileFromText);
    }

    public static PsiElement createCompilerPass(PhpClass bundleClass, String className) throws Exception {

        VirtualFile directory = bundleClass.getContainingFile().getContainingDirectory().getVirtualFile();
        if(fileExists(directory, className)) {
            throw new Exception("File already exists");
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(bundleClass);
        if(scopeForUseOperator == null) {
            throw new Exception("No 'use' scope found");
        }

        VirtualFile compilerDirectory = getAndCreateCompilerDirectory(directory);
        if(compilerDirectory == null) {
            throw new Exception("Directory creation failed");
        }

        Project project = bundleClass.getProject();

        if(bundleClass.findOwnMethodByName("build") == null) {

            insertUseIfNecessary(scopeForUseOperator, "\\Symfony\\Component\\DependencyInjection\\ContainerBuilder");

            Method method = PhpPsiElementFactory.createMethod(project, "" +
                    "public function build(ContainerBuilder $container)\n" +
                    "    {\n" +
                    "        parent::build($container);\n" +
                    "    }"
            );

            PhpCodeEditUtil.insertClassMember(bundleClass, method);
        }

        Method buildMethod = bundleClass.findOwnMethodByName("build");
        if(buildMethod == null) {
            throw new Exception("No 'build' method found");
        }

        String relativePath = VirtualFileUtil.getRelativePath(compilerDirectory, directory);
        if(relativePath == null) {
            throw new Exception("path error");
        }

        MethodReference methodReference = PhpPsiElementFactory.createMethodReference(project, "$container->addCompilerPass(new " + className + "());");

        String ns = bundleClass.getNamespaceName() + relativePath.replace("/", "\\");
        String nsClass = ns + "\\" + className;

        insertUseIfNecessary(scopeForUseOperator, nsClass);

        GroupStatement groupStatement = PhpPsiUtil.getChildByCondition(buildMethod, GroupStatement.INSTANCEOF);
        if(groupStatement != null) {
            PsiElement semicolon = methodReference.getNextSibling();
            groupStatement.addRangeBefore(methodReference, semicolon, groupStatement.getLastChild());
        }

        String COMPILER_TEMPLATE = "/fileTemplates/compiler_pass.php";
        String fileTemplateContent = NewFileActionUtil.getFileTemplateContent(COMPILER_TEMPLATE);
        if(fileTemplateContent == null) {
            throw new Exception("Template content error");
        }

        String replace = fileTemplateContent.replace("{{ ns }}", ns.startsWith("\\") ? ns.substring(1) : ns).replace("{{ class }}", className);
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText(className + ".php", PhpFileType.INSTANCE, replace);
        CodeStyleManager.getInstance(project).reformat(fileFromText);

        return PsiManager.getInstance(project).findDirectory(compilerDirectory).add(fileFromText);
    }

    public static @Nullable PhpClass getPhpClassForCreateCompilerScope(@Nullable PhpClass phpClass) {

        if(phpClass == null) {
            return null;
        }

        if(!PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface")) {
            return null;
        }

        return phpClass;
    }

    public static @Nullable PhpClass getPhpClassForCreateCompilerScope(Editor editor, @Nullable PsiFile file) {
        if(!(file instanceof PhpFile)) {
            return null;
        }

        return getPhpClassForCreateCompilerScope(PhpCodeEditUtil.findClassAtCaret(editor, file));
    }

    private static void insertUseIfNecessary(PhpPsiElement scopeForUseOperator, String nsClass) {
        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).containsValue(nsClass)) {
            PhpAliasImporter.insertUseStatement(nsClass, scopeForUseOperator);
        }
    }

    private static boolean fileExists(VirtualFile bundleDir, String className) {
        return
            VirtualFileUtil.findRelativeFile(bundleDir, "DependencyInjection", "Compiler", className + ".php") != null ||
            VirtualFileUtil.findRelativeFile(bundleDir, "DependencyInjection", "CompilerPass", className + ".php") != null;
    }

    private static boolean fileExists(VirtualFile bundleDir, String... fqnClassName) {

        for (String s : fqnClassName) {
            String[] split = s.split("/");
            split[split.length - 1] += ".php";

            if(VirtualFileUtil.findRelativeFile(bundleDir, split) != null) {
                return true;
            }
        }

        return false;
    }
    private static @Nullable VirtualFile getAndCreateDirectory(VirtualFile directory, String relativePath) {

        try {
            return VirtualFileUtil.createDirectoryIfMissing(directory, relativePath);
        } catch (IOException ignored) {
        }

        return null;
    }

    private static @Nullable VirtualFile getAndCreateCompilerDirectory(VirtualFile directory) {

        VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(directory, "DependencyInjection", "Compiler");
        if(relativeFile != null) {
            return relativeFile;
        }

        relativeFile = VirtualFileUtil.findRelativeFile(directory, "DependencyInjection", "CompilerPass");
        if(relativeFile != null) {
            return relativeFile;
        }

        try {
            return VirtualFileUtil.createDirectoryIfMissing(directory, "DependencyInjection/Compiler");
        } catch (IOException ignored) {
        }

        return null;
    }

}
