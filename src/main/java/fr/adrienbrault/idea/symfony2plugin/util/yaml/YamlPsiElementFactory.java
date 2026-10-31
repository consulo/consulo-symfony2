package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlPsiElementFactory {
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> @Nullable T createFromText(Project p, final Class<T> aClass, String text) {
        final PsiElement[] ret = new PsiElement[]{null};

        createDummyFile(p, text).accept(new PsiRecursiveElementWalkingVisitor() {
            public void visitElement(PsiElement element) {
                if(ret[0] == null && aClass.isInstance(element)) {
                    ret[0] = element;
                }

                super.visitElement(element);
            }
        });

        return (T) ret[0];
    }


    public static PsiFile createDummyFile(Project p, String fileText) {
        return PsiFileFactory.getInstance(p).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, fileText, System.currentTimeMillis(), false);
    }

    public static PsiFile createDummyFile(Project project, String filename, String content) {
        return PsiFileFactory.getInstance(project).createFileFromText(filename, YAMLFileType.YML, content, System.currentTimeMillis(), false);
    }
}
