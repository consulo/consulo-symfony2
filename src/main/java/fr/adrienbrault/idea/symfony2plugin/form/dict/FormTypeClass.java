package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeClass {

    public enum Type {
        PLAIN, ATTRIBUTE
    }

    private final String name;
    private PhpClass phpClass;
    private final String phpClassName;
    private final EnumFormTypeSource source;
    private final Type type;

    public FormTypeClass(String name, String phpClassName, EnumFormTypeSource source, Type type) {
        this.name = name;
        this.phpClassName = phpClassName;
        this.source = source;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public PhpClass getPhpClass(Project project) {
        if(phpClass != null) {
            return phpClass;
        }

        return this.phpClass = PhpElementsUtil.getClass(project, this.phpClassName);
    }

    public EnumFormTypeSource getSource() {
        return source;
    }

    public Type getType() {
        return type;
    }

    public String getPhpClassName() {
        return phpClassName;
    }
}
