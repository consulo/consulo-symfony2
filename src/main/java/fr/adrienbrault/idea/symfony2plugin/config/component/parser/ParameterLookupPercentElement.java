package fr.adrienbrault.idea.symfony2plugin.config.component.parser;

import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupPercentElement extends ParameterLookupElement {

    public ParameterLookupPercentElement(ContainerParameter containerParameter) {
        super(containerParameter);
    }

    public String getLookupString() {
        return "%" + containerParameter.getName() + "%";
    }
}
