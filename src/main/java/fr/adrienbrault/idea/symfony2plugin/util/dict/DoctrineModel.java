package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModel {

    private final PhpClass phpClass;
    private String doctrineNamespace;
    private String doctrineShortcut = null;

    public DoctrineModel(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public DoctrineModel(PhpClass phpClass, @Nullable String doctrineShortcut) {
        this.phpClass = phpClass;
        this.doctrineShortcut = doctrineShortcut;
    }

    public DoctrineModel(PhpClass phpClass, @Nullable String doctrineShortcut, @Nullable String doctrineNamespace) {
        this(phpClass, doctrineShortcut);
        this.doctrineNamespace = doctrineNamespace;
    }

    public @Nullable String getDoctrineShortcut() {
        return doctrineShortcut;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

    public @Nullable String getDoctrineNamespace() {
        return doctrineNamespace;
    }

    public @Nullable String getRepositoryName() {

        String className = phpClass.getPresentableFQN();
        if(doctrineShortcut == null) {
            return className;
        }

        if(doctrineNamespace != null && className.length() > doctrineNamespace.length()) {
            className = className.substring(doctrineNamespace.length());
            return doctrineShortcut + ':'  + className;
        }

        return null;
    }

}
