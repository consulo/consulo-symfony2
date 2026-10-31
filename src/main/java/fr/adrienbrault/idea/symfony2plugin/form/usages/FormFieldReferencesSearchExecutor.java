package fr.adrienbrault.idea.symfony2plugin.form.usages;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Finds builder declarations and attributed form accesses for PHP members.
 */
@ExtensionImpl
public class FormFieldReferencesSearchExecutor implements ReferencesSearchQueryExecutor {
    @Override
    public boolean execute(ReferencesSearch.SearchParameters parameters, Predicate<? super PsiReference> consumer) {
        return ReadAction.compute(() -> {
            if (!(parameters.getElementToSearch() instanceof PhpNamedElement target)) {
                return true;
            }

            boolean completed = FormFieldReferencesSearchExecutorKt.processFormFieldUsages(target, literal ->
                !PsiSearchScopeUtil.isInScope(parameters.getEffectiveSearchScope(), literal) ||
                    consumer.test(new FormFieldUsageReference(literal, target))
            );
            if (!completed) {
                return false;
            }

            return processAttributeFieldAccessUsages(target, parameters, consumer);
        });
    }

    // Find get()/has() accesses separately from classic builder declarations.
    private static boolean processAttributeFieldAccessUsages(PhpNamedElement target, ReferencesSearch.SearchParameters parameters, Predicate<? super PsiReference> consumer) {
        if (!(target instanceof Field field) || !Symfony2ProjectComponent.isEnabled(target.getProject())) {
            return true;
        }

        Set<String> names = FormFieldResolver.getAttributeFieldNames(field);
        if (names.isEmpty()) {
            return true;
        }

        Project project = target.getProject();
        SearchScope searchScope = parameters.getScopeDeterminedByUser();
        GlobalSearchScope globalScope = searchScope instanceof GlobalSearchScope scope ? scope : GlobalSearchScope.projectScope(project);
        GlobalSearchScope phpScope = GlobalSearchScope.getScopeRestrictedByFileTypes(globalScope, PhpFileType.INSTANCE);
        Set<PhpFile> candidates = new LinkedHashSet<>();
        for (String name : names) {
            ProgressManager.checkCanceled();
            PsiSearchHelper.getInstance(project).processAllFilesWithWordInLiterals(name, phpScope, file -> {
                if (file instanceof PhpFile phpFile) {
                    candidates.add(phpFile);
                }
                return true;
            });
        }

        for (PhpFile file : candidates) {
            ProgressManager.checkCanceled();
            for (StringLiteralExpression literal : PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression.class)) {
                if (!names.contains(literal.getContents()) || !PsiSearchScopeUtil.isInScope(searchScope, literal)) {
                    continue;
                }

                for (PsiReference reference : literal.getReferences()) {
                    if (reference instanceof FormFieldNameReference && reference.isReferenceTo(target) && !consumer.test(reference)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
