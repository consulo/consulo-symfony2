package fr.adrienbrault.idea.symfony2plugin.config.component;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupElement extends LookupElement {

    protected Object psiElement;
    protected InsertHandler<LookupElement> insertHandler = null;
    protected final ContainerParameter containerParameter;

    public ParameterLookupElement(ContainerParameter containerParameter) {
        this.containerParameter = containerParameter;
    }

    public ParameterLookupElement(ContainerParameter containerParameter, @Nullable InsertHandler<LookupElement> insertHandler, @Nullable Object psiElement) {
        this.insertHandler = insertHandler;
        this.psiElement = psiElement;
        this.containerParameter = containerParameter;
    }

    @Override
    public Object getObject() {
        return this.psiElement != null ? this.psiElement : this;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    @Override
    public String getLookupString() {
        return containerParameter.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(containerParameter.getName());

        String value = containerParameter.getValue();
        if(value != null && StringUtils.isNotBlank(value)) {
            presentation.setTypeText(value);
        }

        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.PARAMETER);

        if(this.containerParameter.isWeak()) {
            presentation.setIcon(Symfony2Icons.PARAMETER_OPACITY);
        }

    }

}