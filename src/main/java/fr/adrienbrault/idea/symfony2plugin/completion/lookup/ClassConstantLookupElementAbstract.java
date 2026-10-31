package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.insertHandler.ClassConstantInsertHandler;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class ClassConstantLookupElementAbstract extends LookupElement implements ClassConstantInsertHandler.ClassConstantLookupElementInterface {
    protected final PhpClass phpClass;

    String phpClassName;

    public ClassConstantLookupElementAbstract(PhpClass phpClass) {
        this.phpClass = phpClass;
        this.phpClassName = phpClass.getName();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(phpClass.getName());
        presentation.setTypeText(phpClass.getPresentableFQN());
        presentation.setTypeGrayed(true);
        super.renderElement(presentation);
    }

    @Override
    public String getLookupString() {
        return this.phpClassName;
    }

    public Object getObject() {
        return this.phpClass;
    }

    @Override
    public PhpClass getPhpClass() {
        return this.phpClass;
    }

    @Override
    public void handleInsert(InsertionContext context) {
        ClassConstantInsertHandler.getInstance().handleInsert(context, this);
    }
}
