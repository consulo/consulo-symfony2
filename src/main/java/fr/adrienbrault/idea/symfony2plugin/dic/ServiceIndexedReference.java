package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceIndexedReference extends ServiceReference {
    public ServiceIndexedReference(StringLiteralExpression element) {
        super(element, true);
    }
}
