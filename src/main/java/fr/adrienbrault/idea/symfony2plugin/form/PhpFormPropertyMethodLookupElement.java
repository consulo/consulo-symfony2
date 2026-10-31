package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpFormPropertyMethodLookupElement extends PhpLookupElement {

    private final String methodOverwrite;

    PhpFormPropertyMethodLookupElement(PhpNamedElement namedElement, String methodOverwrite) {
        super(namedElement);
        this.methodOverwrite = methodOverwrite;
    }

    public String getLookupString() {
        return this.methodOverwrite;
    }
}
