package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import org.jspecify.annotations.Nullable;

/**
 * A synthetic PsiReference for Twig template usages.
 * Wraps a source element (like an include tag) and resolves to the target TwigFile.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateUsageReference extends PsiReferenceBase<PsiElement> {

    private final PsiElement targetElement;
    private final String templateName;
    private final boolean canRename;

    public TwigTemplateUsageReference(PsiElement sourceElement, PsiElement targetElement, TextRange rangeInElement) {
        this(sourceElement, targetElement, rangeInElement, true);
    }

    public TwigTemplateUsageReference(
            PsiElement sourceElement,
            PsiElement targetElement,
            TextRange rangeInElement,
            boolean canRename) {
        super(sourceElement, rangeInElement, false);
        this.targetElement = targetElement;
        this.templateName = rangeInElement.substring(sourceElement.getText());
        this.canRename = canRename;
    }

    @Override
    public @Nullable PsiElement resolve() {
        return targetElement;
    }

    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
        return myElement;
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        if (!canRename) {
            return myElement;
        }

        String renamedTemplateName = TemplateMoveRenameUtil.renameTemplateName(templateName, newElementName);
        PsiElement result = TemplateMoveRenameUtil.applyRangeReplacement(myElement, getRangeInElement(), renamedTemplateName);
        return result != null ? result : myElement;
    }

    @Override
    public String getCanonicalText() {
        if (targetElement instanceof PsiFile) {
            return ((PsiFile) targetElement).getName();
        }
        return targetElement.getText();
    }
}
