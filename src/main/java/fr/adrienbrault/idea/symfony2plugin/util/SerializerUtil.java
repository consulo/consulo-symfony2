package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.SerializerClassUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import consulo.util.lang.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SerializerUtil {
    public static void visitSerializerMethodReference(PsiFile psiFile, Consumer<Pair<String, PsiElement>> consumer) {
        Collection<MethodReference> methodReferences = new ArrayList<>();

        for (PhpNamedElement topLevelElement : ((PhpFile) psiFile).getTopLevelDefs().values()) {
            if (topLevelElement instanceof PhpClass clazz) {
                for (Method method : clazz.getOwnMethods()) {
                    methodReferences.addAll(PhpElementsUtil.collectMethodReferencesInsideControlFlow(method, "deserialize"));
                }
            }
        }

        for (MethodReference methodReference : methodReferences) {
            PsiElement parameter = methodReference.getParameter(1);
            if (parameter instanceof ClassConstantReference) {
                String classConstantPhpFqn = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) parameter);
                if (StringUtils.isNotBlank(classConstantPhpFqn)) {
                    consumer.accept(new Pair<>("\\" + classConstantPhpFqn.toLowerCase(), parameter));
                }
            } else if (parameter instanceof StringLiteralExpression) {
                String contents = ((StringLiteralExpression) parameter).getContents();
                if (StringUtils.isNotBlank(contents)) {
                    if (!contents.startsWith("\\")){
                        contents = "\\" + contents;
                    }

                    if (contents.endsWith("[]")) {
                        contents = contents.substring(0, contents.length() - 2);
                    }

                    if (StringUtils.isNotBlank(contents)) {
                        consumer.accept(new Pair<>(contents.toLowerCase(), parameter));
                    }
                }
            } else if (parameter instanceof ConcatenationExpression) {
                PsiElement[] children = parameter.getChildren();
                if (children[0] instanceof ClassConstantReference && children[1] instanceof StringLiteralExpression) {
                    String classConstantPhpFqn = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) children[0]);
                    if (StringUtils.isNotBlank(classConstantPhpFqn)) {
                        String contents = ((StringLiteralExpression) children[1]).getContents();
                        if (contents.equals("[]")) {
                            consumer.accept(new Pair<>("\\" + classConstantPhpFqn.toLowerCase(), parameter));
                        }
                    }
                }
            }
        }
    }

    public static Collection<PsiElement> getClassTargetForSerializer(Project project, String className) {
        final Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(SerializerClassUsageStubIndex.KEY, new HashSet<>(Collections.singletonList(className.toLowerCase())), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        Collection<PsiElement> psiElements = new ArrayList<>();

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles)) {
            visitSerializerMethodReference(psiFile, pair -> {
                if (className.toLowerCase().equals(pair.getFirst())) {
                    psiElements.add(pair.getSecond());
                }
            });
        }

        return psiElements;
    }

    public static boolean hasClassTargetForSerializer(Project project, String className) {
        return IndexUtil.getAllKeysForProject(SerializerClassUsageStubIndex.KEY, project).contains(className.toLowerCase());
    }
}
