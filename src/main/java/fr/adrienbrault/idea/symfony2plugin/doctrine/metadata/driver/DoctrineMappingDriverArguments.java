package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMappingDriverArguments {

    private final Project project;

    private final PsiFile psiFile;

    private final String fqnClass;

    /**
     * @param fqnClass fully qualified entity class name; leading backslash is added if missing
     */
    public DoctrineMappingDriverArguments(Project project, PsiFile psiFile, String fqnClass) {
        this.project = project;
        this.psiFile = psiFile;
        this.fqnClass = fqnClass.startsWith("\\") ? fqnClass : "\\" + fqnClass;
    }

    public Project getProject() {
        return project;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    /**
     * @return fully qualified entity class name with leading backslash
     */
    public String getClassName() {
        return fqnClass;
    }

    public boolean isEqualClass(@Nullable String fqnClass) {
        if (fqnClass == null) {
            return false;
        }

        // normalize the incoming class name to also have a leading backslash before comparing
        if (!fqnClass.startsWith("\\")) {
            fqnClass = "\\" + fqnClass;
        }

        return this.fqnClass.equals(fqnClass);
    }
}
