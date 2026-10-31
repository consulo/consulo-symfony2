package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import consulo.language.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateInclude {
    private final PsiElement psiElement;

    private final String templateName;

    private final TYPE type;

    public enum TYPE {
        INCLUDE, INCLUDE_FUNCTION, EMBED, IMPORT, FROM, FORM_THEME, BLOCK_FUNCTION, SOURCE_FUNCTION
    }

    public TemplateInclude(PsiElement psiElement, String templateName, TYPE type) {
        this.psiElement = psiElement;
        this.templateName = templateName;
        this.type = type;
    }

    public PsiElement getPsiElement() {
        return psiElement;
    }

    public String getTemplateName() {
        return templateName;
    }

    public TYPE getType() {
        return type;
    }
}
