package fr.adrienbrault.idea.symfony2plugin.templating;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.language.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {
    private final String templateName;

    public TemplateReference(StringLiteralExpression element) {
        super(element);
        templateName = element.getContents();
    }

    @Override
    public Object[] getCompletionVariants() {
        return TwigUtil.getAllTemplateLookupElements(getElement().getProject()).toArray();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult
            .createResults(TwigUtil.getTemplatePsiElements(getElement().getProject(), templateName));
    }

    /**
     * Called by IntelliJ's move-file refactoring to update the template path when the referenced
     * Twig file is moved to a new location.
     */
    @Override
    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
        if (!(element instanceof PsiFile newFile)) {
            return getElement();
        }

        VirtualFile newVirtualFile = newFile.getVirtualFile();
        if (newVirtualFile == null) {
            return getElement();
        }

        String newTemplateName = TemplateMoveRenameUtil.resolveNewTemplateName(
            getElement().getProject(), newVirtualFile, templateName
        );
        if (newTemplateName == null) {
            return getElement();
        }

        PsiElement result = TemplateMoveRenameUtil.applyToStringLiteralElement(getElement(), newTemplateName);
        return result != null ? result : getElement();
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        String newTemplateName = TemplateMoveRenameUtil.renameTemplateName(templateName, newElementName);
        PsiElement result = TemplateMoveRenameUtil.applyToStringLiteralElement(getElement(), newTemplateName);
        return result != null ? result : getElement();
    }
}
