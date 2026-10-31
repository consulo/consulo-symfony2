package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import consulo.language.editor.completion.lookup.InsertionContext;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationUseImporter;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpConstantFieldPhpLookupElement extends PhpLookupElement {

    final private PhpClass phpClass;

    public PhpConstantFieldPhpLookupElement(Field field) {
        super(field);
        this.phpClass = field.getContainingClass();
    }

    @Override
    public Object getObject() {
        return this.phpClass;
    }

    @Override
    public String getLookupString() {
        return this.phpClass.getName() + "::" + super.getLookupString();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        AnnotationUseImporter.insertUse(context, phpClass.getFQN());
    }

}
