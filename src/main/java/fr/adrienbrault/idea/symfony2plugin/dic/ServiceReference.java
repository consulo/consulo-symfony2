package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceReference extends AbstractServiceReference {

    public ServiceReference(StringLiteralExpression element) {
        super(element);
        this.serviceId = element.getContents();
    }

    public ServiceReference(StringLiteralExpression element, boolean usePrivateServices) {
        this(element);
        this.usePrivateServices = usePrivateServices;
    }

}
