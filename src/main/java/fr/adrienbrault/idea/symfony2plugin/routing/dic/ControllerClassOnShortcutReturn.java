package fr.adrienbrault.idea.symfony2plugin.routing.dic;

import com.jetbrains.php.lang.psi.elements.PhpClass;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerClassOnShortcutReturn {

    private final PhpClass phpClass;
    private boolean isService = false;

    public ControllerClassOnShortcutReturn(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public ControllerClassOnShortcutReturn(PhpClass phpClass, boolean isService) {
        this.phpClass = phpClass;
        this.isService = isService;
    }

    public boolean isService() {
        return isService;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

}
