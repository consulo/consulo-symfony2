package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineRepositoryLookupElement extends LookupElement {
    private final String phpClassPresentableFQN;

    private final String phpClassName;

    private DoctrineRepositoryLookupElement(PhpClass phpClass) {
        this.phpClassName = phpClass.getName();
        this.phpClassPresentableFQN = phpClass.getPresentableFQN();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(phpClassName);
        presentation.setTypeText(phpClassPresentableFQN);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.DOCTRINE);
    }

    @Override
    public String getLookupString() {
        return StringUtils.stripStart(phpClassPresentableFQN, "\\");
    }

    public static DoctrineRepositoryLookupElement create(PhpClass phpClass) {
        return new DoctrineRepositoryLookupElement(phpClass);
    }

    public static Collection<DoctrineRepositoryLookupElement> create(Collection<PhpClass> phpClasses) {
        Collection<DoctrineRepositoryLookupElement> elements = new ArrayList<>();
        for(PhpClass phpClass: phpClasses) {
            elements.add(DoctrineRepositoryLookupElement.create(phpClass));
        }

        return elements;
    }
}
