package fr.adrienbrault.idea.symfony2plugin.config;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.project.DumbService;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassPublicMethodReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {
    private final String className;
    private final Project project;

    public ClassPublicMethodReference(PsiElement element, String className) {
        super(element);
        this.className = className;
        this.project = element.getProject();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        if (DumbService.isDumb(project)) {
            return ResolveResult.EMPTY_ARRAY;
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, this.className);
        if(phpClass == null) {
            return new ResolveResult[0];
        }

        Method targetMethod = phpClass.findMethodByName(PsiElementUtils.trimQuote(getElement().getText()));
        if(targetMethod == null) {
            return new ResolveResult[0];
        }

        return PsiElementResolveResult.createResults(targetMethod);
    }

    @Override
    @Deprecated
    public Object[] getCompletionVariants() {
        if (DumbService.isDumb(project)) {
            return new Object[0];
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, this.className);
        if(phpClass == null) {
            return new Object[0];
        }

        List<LookupElement> lookupElements = new ArrayList<>();
        for(Method publicMethod: PhpElementsUtil.getClassPublicMethod(phpClass)) {
            lookupElements.add(new PhpLookupElement(publicMethod));
        }

        return lookupElements.toArray();
    }
}
