package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormClass {

    private final FormClassEnum typeEnum;
    private final PhpClass phpClass;
    private final boolean isWeak;

    public FormClass(FormClassEnum typeEnum, PhpClass phpClass, boolean isWeak) {
        this.typeEnum = typeEnum;
        this.phpClass = phpClass;
        this.isWeak = isWeak;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

    public boolean isWeak() {
        return isWeak;
    }

    public FormClassEnum getType() {
        return typeEnum;
    }

}
