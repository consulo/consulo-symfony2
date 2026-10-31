package fr.adrienbrault.idea.symfony2plugin.util.controller;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerReference extends PsiReferenceBase<PsiElement> implements SymfonyCompletionReference, PsiReference {
    private final Project project;
    private final StringLiteralExpression element;

    public ControllerReference(StringLiteralExpression element) {
        super(element);
        this.element = element;
        this.project = element.getProject();
    }

    @Override
    public @Nullable PsiElement resolve() {

        // @TODO: multiresolve

        String contents = element.getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(this.project, contents);
        if(methods.length > 0) {
            return methods[0];
        }

        return null;
    }

    @Override
    public Object[] getCompletionVariants() {
        return ControllerIndex.getControllerLookupElements(this.project).toArray();
    }
}
