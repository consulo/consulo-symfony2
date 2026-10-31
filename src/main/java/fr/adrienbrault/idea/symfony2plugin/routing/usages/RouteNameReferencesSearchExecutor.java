package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndexKt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Collects route usages for Find Usages.
 * Twig usages come from the stub index, PHP usages are found by literal word search and filtered down to real route references.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RouteNameReferencesSearchExecutor implements ReferencesSearchQueryExecutor {
    /**
     * Entry point used by IntelliJ's reference search infrastructure for route declarations.
     */
    @Override
    public boolean execute(ReferencesSearch.SearchParameters queryParameters, Predicate<? super PsiReference> consumer) {
        Application.get().runReadAction((Runnable) () -> {
            PsiElement target = queryParameters.getElementToSearch();
            String routeName = RouteUsageUtil.getRouteNameForDeclaration(target);
            if (routeName == null) {
                return;
            }

            PsiElement declarationTarget = RouteUsageUtil.getRouteDeclarationTarget(target);
            if (declarationTarget == null) {
                return;
            }

            doSearch(target.getProject(), routeName, declarationTarget, queryParameters, consumer);
        });

        return true;
    }

    /**
     * Runs the actual search over Twig and PHP files and emits synthetic references to the declaration target.
     */
    private void doSearch(
        Project project,
        String routeName,
        PsiElement declarationTarget,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        FileBasedIndex index = FileBasedIndex.getInstance();
        SearchScope searchScope = queryParameters.getEffectiveSearchScope();
        GlobalSearchScope scope = searchScope instanceof GlobalSearchScope
            ? (GlobalSearchScope) searchScope
            : GlobalSearchScope.projectScope(project);
        GlobalSearchScope twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE);
        PsiManager psiManager = PsiManager.getInstance(project);
        Set<PsiElement> processed = new HashSet<>();

        index.processValues(TwigRouteUsageStubIndex.KEY, routeName, null, (file, value) -> {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile == null) {
                return true;
            }

            if (!(psiFile instanceof TwigFile)) {
                return true;
            }

            for (TwigRouteUsageStubIndex.Usage usage : TwigRouteUsageStubIndexKt.getTwigRouteUsages((TwigFile) psiFile, Collections.singleton(routeName))) {
                if (!processed.add(usage.getTarget())) {
                    continue;
                }

                consumer.test(new RouteUsageReference(usage.getTarget(), declarationTarget, new TextRange(0, usage.getTarget().getTextLength())));
            }

            return true;
        }, twigScope);

        PsiSearchHelper.getInstance(project).processAllFilesWithWordInLiterals(routeName, scope, psiFile -> {
            if (!(psiFile instanceof PhpFile)) {
                return true;
            }

            for (StringLiteralExpression literalExpression : PsiTreeUtil.findChildrenOfType(psiFile, StringLiteralExpression.class)) {
                if (!routeName.equals(literalExpression.getContents()) || !processed.add(literalExpression)) {
                    continue;
                }

                if (!isPhpRouteUsage(literalExpression)) {
                    continue;
                }

                consumer.test(
                    new RouteUsageReference(
                        literalExpression,
                        declarationTarget,
                        new TextRange(1, 1 + routeName.length())
                    )
                );
            }

            return true;
        });
    }

    /**
     * Guards the PHP literal scan so only literals that already resolve as route references are counted as usages.
     */
    private boolean isPhpRouteUsage(StringLiteralExpression literalExpression) {
        for (PsiReference reference : literalExpression.getReferences()) {
            if (reference instanceof RouteReference) {
                return true;
            }
        }

        return false;
    }

    /**
     * Synthetic reference used for usages produced by the custom route search pipeline instead of native PSI references.
     */
    public static class RouteUsageReference extends PsiReferenceBase<PsiElement> {
        private final PsiElement myTargetElement;

        public RouteUsageReference(PsiElement sourceElement, PsiElement targetElement, TextRange rangeInElement) {
            super(sourceElement, rangeInElement, false);
            myTargetElement = targetElement;
        }

        /**
         * Resolves the synthetic usage back to the route declaration PSI.
         */
        @Override
        public PsiElement resolve() {
            return myTargetElement;
        }

        /**
         * Find Usages does not need completion variants for these synthetic references.
         */
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }
}
